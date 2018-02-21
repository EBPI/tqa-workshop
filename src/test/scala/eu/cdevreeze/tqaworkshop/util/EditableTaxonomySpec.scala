/*
 * Copyright 2016-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.tqaworkshop.util

import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames.NameEName
import eu.cdevreeze.tqa.ENames.TypeEName
import eu.cdevreeze.tqa.ENames.XbrliPeriodTypeEName
import eu.cdevreeze.tqa.ENames.XsElementEName
import eu.cdevreeze.tqa.ENames.XsImportEName
import eu.cdevreeze.tqa.ENames.XsSchemaEName
import eu.cdevreeze.tqa.Namespaces.XbrliNamespace
import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.backingelem.indexed.docbuilder.IndexedDocumentBuilder
import eu.cdevreeze.tqa.base.dom.GlobalElementDeclaration
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.TaxonomyElem
import eu.cdevreeze.tqa.base.taxonomy.BasicTaxonomy
import eu.cdevreeze.tqa.docbuilder.jvm.UriConverters
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.resolved

/**
 * Test specification for taxonomy editing.
 *
 * For the test data, see http://xbrl.squarespace.com/journal/2008/12/18/hello-world-xbrl-example.html.
 *
 * @author Chris de Vreeze
 */
class EditableTaxonomySpec extends FlatSpec {

  private val docParser = DocumentParserUsingDom.newInstance()
  private val docBuilder =
    new IndexedDocumentBuilder(docParser, UriResolvers.fromUriConverter(UriConverters.identity))

  private val xsdTemplateBackingDoc: BackingDocumentApi =
    docBuilder.build(classOf[EditableTaxonomySpec].getResource("HelloWorld-template.xsd").toURI)

  private val elemDeclTemplateElem: BackingElemApi = {
    val doc =
      docBuilder.build(classOf[EditableTaxonomySpec].getResource("HelloWorld-element-template.xsd").toURI)

    doc.documentElement.findChildElem(_.localName == "element").head
  }

  private def getFallbackPrefix(ns: String): String = sys.error(s"No prefix generation supported!")
  private val backingElemEditor = new IndexedElemEditor(getFallbackPrefix _)

  "The taxonomy editor" should "allow insertion of global element declarations" in {
    val initialTaxoBase = TaxonomyBase.build(Vector(TaxonomyDocument.build(xsdTemplateBackingDoc)))
    val initialEditableTaxo = new EditableTaxonomy(initialTaxoBase, backingElemEditor)

    val elemDecls = Vector(
      makeItemDeclaration("Land", EName(XbrliNamespace, "monetaryItemType"), "instant"),
      makeItemDeclaration("BuildingsNet", EName(XbrliNamespace, "monetaryItemType"), "instant"),
      makeItemDeclaration("FurnitureAndFixturesNet", EName(XbrliNamespace, "monetaryItemType"), "instant"),
      makeItemDeclaration("ComputerEquipmentNet", EName(XbrliNamespace, "monetaryItemType"), "instant"),
      makeItemDeclaration("OtherPropertyPlantAndEquipmentNet", EName(XbrliNamespace, "monetaryItemType"), "instant"),
      makeItemDeclaration("PropertyPlantAndEquipmentNet", EName(XbrliNamespace, "monetaryItemType"), "instant"))

    // Perform the taxonomy edit action
    val resultEditableTaxo = initialEditableTaxo.addGlobalElementDeclarations(elemDecls)

    val resultTaxo = BasicTaxonomy.build(
      resultEditableTaxo.taxonomyBase,
      SubstitutionGroupMap.Empty,
      Vector())

    assertResult(elemDecls.size) {
      resultTaxo.findAllItemDeclarations.size
    }
    assertResult(elemDecls.map(_.targetEName)) {
      resultTaxo.findAllItemDeclarations.map(_.targetEName)
    }

    assertResult(Vector(
      Path.Empty,
      Path.from(XsImportEName -> 0),
      Path.from(XsElementEName -> 0),
      Path.from(XsElementEName -> 1),
      Path.from(XsElementEName -> 2),
      Path.from(XsElementEName -> 3),
      Path.from(XsElementEName -> 4),
      Path.from(XsElementEName -> 5))) {

      resultTaxo.taxonomyBase.rootElems.head.findAllElemsOrSelf.map(_.backingElem.path)
    }

    val expectedResultElem: indexed.Elem =
      docBuilder.build(classOf[EditableTaxonomySpec].getResource("HelloWorld.xsd").toURI).documentElement

    assertResult(resolved.Elem(expectedResultElem.underlyingElem.prettify(2))) {
      resolved.Elem(
        resultTaxo.taxonomyBase.rootElems.head.backingElem.asInstanceOf[indexed.Elem].underlyingElem.prettify(2))
    }
  }

  private def makeItemDeclaration(name: String, tpe: EName, periodType: String): GlobalElementDeclaration = {
    require(elemDeclTemplateElem.resolvedName == XsElementEName)
    require(elemDeclTemplateElem.rootElem.resolvedName == XsSchemaEName)
    require(tpe.namespaceUriOption.isDefined, s"Expected type in a namespace, but got $tpe")

    val backingElem1 =
      backingElemEditor.plusResolvedAttribute(elemDeclTemplateElem.asInstanceOf[indexed.Elem], NameEName, name)

    val backingElem2 = backingElemEditor.includingNamespaceOption(backingElem1, tpe.namespaceUriOption)

    // TODO Add support for adding QName-valued attributes etc.
    val prefixOption =
      backingElem2.scope.withoutDefaultNamespace.prefixesForNamespace(tpe.namespaceUriOption.get).headOption
    val backingElem3 =
      backingElemEditor.plusResolvedAttribute(backingElem2, TypeEName, tpe.toQName(prefixOption).toString)

    val backingElem4 =
      backingElemEditor.plusResolvedAttribute(backingElem3, XbrliPeriodTypeEName, periodType)

    TaxonomyElem.build(backingElem4).asInstanceOf[GlobalElementDeclaration]
  }
}
