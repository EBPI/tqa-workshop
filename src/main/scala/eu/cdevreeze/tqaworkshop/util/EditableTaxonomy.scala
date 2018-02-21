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

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.tqa.base.dom.GlobalElementDeclaration
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.TaxonomyElem
import eu.cdevreeze.tqa.base.dom.XsdSchema
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.BackingElemApi

/**
 * Simple functionally editable taxonomy. The user of this class is responsible for choosing the same
 * backing element type as used underneath by the taxonomy!
 *
 * @author Chris de Vreeze
 */
final class EditableTaxonomy[N, E <: N with BackingElemApi.Aux[E]](
  val taxonomyBase:      TaxonomyBase,
  val backingElemEditor: BackingElemEditor.Aux[N, E]) {

  def addGlobalElementDeclarations(decls: immutable.IndexedSeq[GlobalElementDeclaration]): EditableTaxonomy[N, E] = {
    require(
      decls.size == decls.map(_.targetEName).distinct.size,
      s"Trying to add the same global element declarations more than once")

    val duplicateElemENames: Set[EName] =
      decls.map(_.targetEName).toSet.intersect(taxonomyBase.globalElementDeclarationMap.keySet)

    require(duplicateElemENames.isEmpty, s"Duplicate global element declarations compared to the existing ones. For example: ${duplicateElemENames.head}")

    val docUrisByTnsOption: Map[Option[String], URI] = getDocUrisByTnsOption(decls)

    val declsByUri: Map[URI, immutable.IndexedSeq[GlobalElementDeclaration]] =
      decls groupBy { decl =>
        docUrisByTnsOption.getOrElse(decl.targetEName.namespaceUriOption, sys.error(s"No document for target namespace option ${decl.targetEName.namespaceUriOption}"))
      }

    val updates: Map[URI, TaxonomyElem] =
      declsByUri map {
        case (docUri, declGroup) =>
          val oldTaxoElem = taxonomyBase.rootElemUriMap.getOrElse(docUri, sys.error(s"Missing xs:schema at $docUri"))

          // The update action itself
          val resultTaxoElem: TaxonomyElem =
            updateTaxonomyElem(oldTaxoElem, { e =>
              val rawElem =
                backingElemEditor.plusChildren(e, declGroup.map(_.backingElem.asInstanceOf[E]))

              val nonPrettifiedElem =
                backingElemEditor.pushUpPrefixedNamespaces(rawElem)

              val prettifiedElem =
                backingElemEditor.prettify(nonPrettifiedElem, 2)
              prettifiedElem
            })

          (docUri -> resultTaxoElem)
      }

    val resultTaxoBase: TaxonomyBase = updateTaxonomyBase(updates)

    new EditableTaxonomy(resultTaxoBase, backingElemEditor)
  }

  private def getDocUrisByTnsOption(decls: immutable.IndexedSeq[GlobalElementDeclaration]): Map[Option[String], URI] = {
    decls.groupBy(_.targetEName.namespaceUriOption) mapValues { declGroup =>
      val tnsOption = declGroup.head.targetEName.namespaceUriOption

      val uriOption = taxonomyBase.rootElems collectFirst {
        case e: XsdSchema if e.targetNamespaceOption == tnsOption => e.docUri
      }

      uriOption.getOrElse(sys.error(s"Missing document for TNS option $tnsOption"))
    }
  }

  private def updateTaxonomyElem(taxoElem: TaxonomyElem, f: E => E): TaxonomyElem = {
    TaxonomyElem.build(f(taxoElem.backingElem.asInstanceOf[E]))
  }

  private def updateTaxonomyBase(updates: Map[URI, TaxonomyElem]): TaxonomyBase = {
    val newRootElems = taxonomyBase.rootElems map { rootElem =>
      updates.getOrElse(rootElem.docUri, rootElem)
    }

    TaxonomyBase.build(newRootElems.map(e => TaxonomyDocument(None, e)))
  }
}
