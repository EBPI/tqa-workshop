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

package eu.cdevreeze.tqaworkshop.chapter2

import java.io.File
import java.net.URI

import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames.IdEName
import eu.cdevreeze.tqa.backingelem.nodeinfo.docbuilder.SaxonDocumentBuilder
import eu.cdevreeze.tqa.base.dom.ChildSequencePointer
import eu.cdevreeze.tqa.base.dom.IdChildSequencePointer
import eu.cdevreeze.tqa.base.dom.IdPointer
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.TaxonomyElem
import eu.cdevreeze.tqa.base.dom.XLinkLocator
import eu.cdevreeze.tqa.base.dom.XPointer
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import net.sf.saxon.s9api.Processor

/**
 * Test specification for (XBRL) XPointer processing, in an XLink context. In this test case, the low level type-safe
 * taxonomy DOM in TQA is shown, but almost exclusively at the level of XLink content.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 *
 * Study the input taxonomy files as well, because the test methods use this input. Note that they
 * occur in the taxonomy-edited tree!
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class XPointerSpec extends FlatSpec {

  private val taxoRootDir = new File(classOf[XPointerSpec].getResource("/taxonomy-edited").toURI)

  // Parsing the taxonomy files into a TQA model with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)
  private val docBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromLocalMirrorRootDirectory(taxoRootDir))

  private val schemaUri =
    URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd")
  private val linkbaseUri =
    URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-verbose-lab-en-edited.xml")

  private val taxonomyBase: TaxonomyBase = {
    val schemaBackingDoc = docBuilder.build(schemaUri)
    val linkbaseBackingDoc = docBuilder.build(linkbaseUri)

    val schemaDoc = TaxonomyDocument.build(schemaBackingDoc)
    val linkbaseDoc = TaxonomyDocument.build(linkbaseBackingDoc)

    // Return a TQA TaxonomyBase object, containing the schema and linkbase as TQA type-safe DOM trees.
    // Due to the way they have been parsed, they contain the original HTTP document URIs, although they have been
    // parsed from the local file system.

    TaxonomyBase.build(Vector(schemaDoc, linkbaseDoc))
  }

  //
  // Exercise 1
  //

  "An ID XPointer" should "resolve to an XML element" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val locatorOption =
      linkbase.findElemOfType(classTag[XLinkLocator])(_.xlinkLabel == "rj-i_ConstructionContractsAssets_loc")

    assertResult(true) {
      locatorOption.isDefined
    }

    val locator = locatorOption.get

    // Using the base URI, so respecting XML Base processing

    val absoluteLocatorHref = locator.baseUri.resolve(locator.rawHref)

    assertResult(URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#element(rj-i_ConstructionContractsAssets)")) {
      absoluteLocatorHref
    }

    val xpointer = XPointer.parse(absoluteLocatorHref.getFragment).asInstanceOf[IdPointer]

    assertResult("rj-i_ConstructionContractsAssets") {
      xpointer.id
    }

    assertResult(true) {
      taxonomyBase.findElemByUri(absoluteLocatorHref).isDefined
    }

    assertResult(Some("rj-i_ConstructionContractsAssets")) {
      taxonomyBase.findElemByUri(absoluteLocatorHref).flatMap(_.attributeOption(IdEName))
    }

    // Implement function findElem yourself, using function XPointer.findElem in the XPointer companion object.
    // All hard work is done by method XPointer.findElem (in the XPointer companion object), so there is no effort
    // needed to implement navigating the element tree according to the XPointer.
    // Hint: study the TaxonomyBase API in order to get the root element given a document URI.

    def findElem(docUri: URI, idPointer: IdPointer): Option[TaxonomyElem] = {
      require(docUri.getFragment == null, s"Expected no fragment in (document) URI $docUri")

      ???
    }

    assertResult(taxonomyBase.findElemByUri(absoluteLocatorHref)) {
      findElem(withoutFragment(absoluteLocatorHref), xpointer)
    }
  }

  //
  // Exercise 2
  //

  "A child sequence XPointer" should "resolve to an XML element" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val locatorOption =
      linkbase.findElemOfType(classTag[XLinkLocator])(_.xlinkLabel == "rj-i_AccommodationCosts_loc")

    assertResult(true) {
      locatorOption.isDefined
    }

    val locator = locatorOption.get

    // Using the base URI, so respecting XML Base processing

    val absoluteLocatorHref = locator.baseUri.resolve(locator.rawHref)

    assertResult(
      URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#element(/1/11)")) {

        absoluteLocatorHref
      }

    val xpointer = XPointer.parse(absoluteLocatorHref.getFragment).asInstanceOf[ChildSequencePointer]

    assertResult(List(1, 11)) {
      xpointer.childSequence
    }

    assertResult(true) {
      taxonomyBase.findElemByUri(absoluteLocatorHref).isDefined
    }

    assertResult(Some("rj-i_AccommodationCosts")) {
      taxonomyBase.findElemByUri(absoluteLocatorHref).flatMap(_.attributeOption(IdEName))
    }

    // Implement function findElem yourself, using function XPointer.findElem in the XPointer companion object.
    // All hard work is done by method XPointer.findElem (in the XPointer companion object), so there is no effort
    // needed to implement navigating the element tree according to the XPointer.
    // If the findElem method of the preceding exercise is applicable here, feel free to use the same code again.

    def findElem(docUri: URI, idPointer: ChildSequencePointer): Option[TaxonomyElem] = {
      require(docUri.getFragment == null, s"Expected no fragment in (document) URI $docUri")

      ???
    }

    assertResult(taxonomyBase.findElemByUri(absoluteLocatorHref)) {
      findElem(withoutFragment(absoluteLocatorHref), xpointer)
    }
  }

  //
  // Exercise 3
  //

  "An ID child sequence XPointer" should "resolve to an XML element" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val locatorOption =
      linkbase.findElemOfType(classTag[XLinkLocator])(_.xlinkLabel == "rj-i_AccommodationCostsDisclosure_loc")

    assertResult(true) {
      locatorOption.isDefined
    }

    val locator = locatorOption.get

    // Using the base URI, so respecting XML Base processing

    val absoluteLocatorHref = locator.baseUri.resolve(locator.rawHref)

    assertResult(
      URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#element(rj-data/12)")) {

        absoluteLocatorHref
      }

    val xpointer = XPointer.parse(absoluteLocatorHref.getFragment).asInstanceOf[IdChildSequencePointer]

    assertResult(("rj-data", List(12))) {
      (xpointer.id, xpointer.childSequence)
    }

    assertResult(true) {
      taxonomyBase.findElemByUri(absoluteLocatorHref).isDefined
    }

    assertResult(Some("rj-i_AccommodationCostsDisclosure")) {
      taxonomyBase.findElemByUri(absoluteLocatorHref).flatMap(_.attributeOption(IdEName))
    }

    // Implement function findElem yourself, using function XPointer.findElem in the XPointer companion object.
    // All hard work is done by method XPointer.findElem (in the XPointer companion object), so there is no effort
    // needed to implement navigating the element tree according to the XPointer.
    // If the findElem method of the preceding exercise is applicable here, feel free to use the same code again.

    def findElem(docUri: URI, idPointer: IdChildSequencePointer): Option[TaxonomyElem] = {
      require(docUri.getFragment == null, s"Expected no fragment in (document) URI $docUri")

      ???
    }

    assertResult(taxonomyBase.findElemByUri(absoluteLocatorHref)) {
      findElem(withoutFragment(absoluteLocatorHref), xpointer)
    }
  }

  private def withoutFragment(uri: URI): URI = {
    new URI(uri.getScheme, uri.getSchemeSpecificPart, null)
  }
}
