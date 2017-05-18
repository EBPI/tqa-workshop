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

package eu.cdevreeze.tqaworkshop.chapter1

import scala.collection.immutable

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames.DimensionEName
import eu.cdevreeze.tqa.ENames.IdEName
import eu.cdevreeze.tqa.Namespaces.LinkNamespace
import eu.cdevreeze.tqa.Namespaces.XbrliNamespace
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import net.sf.saxon.s9api.Processor

/**
 * Test specification for low level yaidom querying, at the abstraction level of XML.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the yaidom library available.
 * Also see [[http://dvreeze.github.io/yaidom-queries.html]] and [[http://dvreeze.github.io/yaidom-and-namespaces.html]].
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class QuerySpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi" element with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)
  private val docBuilder = new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uri))

  private val rootElem: BackingElemApi =
    docBuilder.build(classOf[QuerySpec].getResource("/sample-Instance-Proof.xml").toURI)

  private val XbrldiNamespace = "http://xbrl.org/2006/xbrldi"
  private val Iso4217Namespace = "http://www.xbrl.org/2003/iso4217"

  private val GaapNamespace = "http://xasb.org/gaap"

  // In the tests below, use ENames and not (lexical) QNames in queries and element predicates.
  // Feel free to create EName constants where needed.

  "The query API" should "support filtering of child elements" in {
    // Semantic query: Find all XBRL contexts whose ID starts with the string "I-2007".

    // Yaidom query: Filter all child elements of the root element named xbrli:context, having an ID attribute
    // starting with string "I-2007".

    def hasIdStartingWithI2007(elem: BackingElemApi): Boolean = {
      elem.attributeOption(IdEName).exists(_.startsWith("I-2007"))
    }

    // Implement the following function

    def isContextHavingIdStartingWithI2007(elem: BackingElemApi): Boolean = ???

    // Method filterChildElems filters child elements, like the name suggests.
    // An element predicate ("filter") is passed as argument.
    // Like the name says, only element nodes are returned.

    val filteredContexts: immutable.IndexedSeq[BackingElemApi] =
      rootElem.filterChildElems(isContextHavingIdStartingWithI2007)

    assertResult(26) {
      filteredContexts.size
    }
  }

  it should "support filtering of descendant elements" in {
    // Semantic query: Find all explicit members in XBRL contexts.

    // Yaidom query: Filter all descendant elements of the root element named xbrldi:explicitMember.

    // Implement the following function

    def isExplicitMember(elem: BackingElemApi): Boolean = ???

    // Method filterElems filters descendant elements; the word "descendant" is implicit in the name.
    // An element predicate ("filter") is passed as argument.
    // Like the name says, only element nodes are returned.

    val explicitMembers: immutable.IndexedSeq[BackingElemApi] =
      rootElem.filterElems(isExplicitMember)

    assertResult(Set("explicitMember")) {
      explicitMembers.map(_.localName).toSet
    }

    assertResult(true) {
      val dimensions: Set[EName] =
        explicitMembers.flatMap(_.attributeAsResolvedQNameOption(DimensionEName)).toSet

      val someDimensions: Set[EName] =
        List("EntityAxis", "BusinessSegmentAxis", "VerificationAxis", "PremiseAxis", "ReportDateAxis").
          map(localName => EName(GaapNamespace, localName)).toSet

      someDimensions.subsetOf(dimensions)
    }
  }

  it should "support filtering of descendant-or-self elements" in {
    // Semantic query: Find all elements in the xbrli namespace.

    // Yaidom query: Filter all descendant-or-self elements of the root element in the xbrli namespace.

    // Implement the following function

    def isInXbrliNamespace(elem: BackingElemApi): Boolean = ???

    // Method filterElemsOrSelf filters descendant-or-self elements; the word "descendant" is implicit in the name.
    // An element predicate ("filter") is passed as argument.
    // Like the name says, only element nodes are returned.

    val xbrliElems: immutable.IndexedSeq[BackingElemApi] =
      rootElem.filterElemsOrSelf(isInXbrliNamespace)

    assertResult(true) {
      val xbrliENames = xbrliElems.map(_.resolvedName).toSet

      val someXbrliENames: Set[EName] =
        List("xbrl", "context", "entity", "identifier", "segment", "period", "unit").
          map(localName => EName(XbrliNamespace, localName)).toSet

      someXbrliENames.subsetOf(xbrliENames)
    }

    assertResult(true) {
      xbrliElems.filter(_.resolvedName.namespaceUriOption.contains(GaapNamespace)).isEmpty
    }
  }

  it should "support retrieval of attributes" in {
    // Semantic query: Find all XBRL unit IDs.

    // Yaidom query: Find all xbrli:unit element ID attributes.
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following variable

    val unitIds: Set[String] = ???

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitIds
    }
  }

  it should "support retrieval of optional attributes" in {
    // Semantic query: Find all numeric item fact unitRefs.

    // Yaidom query: Find all unitRef attributes in descendant elements of the root element.
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following variable

    val unitRefs: Set[String] = ???

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitRefs
    }
  }

  it should "support retrieval of element texts" in {
    // Semantic query: Find all gaap:RelatedPartyTypeOfRelationship fact values.

    // Yaidom query: Find all texts of descendant elements of the root element that have
    // element name gaap:RelatedPartyTypeOfRelationship.

    // Implement the following variable

    val interestingFactValues: Set[String] = ???

    assertResult(Set("Parent", "JointVenture")) {
      interestingFactValues
    }
  }

  it should "support retrieval of QName-valued texts" in {
    // Semantic query: Find all measures (as expanded names).

    // Yaidom query: Find all texts as ENames of descendant elements of the root element that have
    // element name xbrli:measure.

    // Implement the following variable

    val measureNames: Set[EName] = ???

    assertResult(Set(EName(Iso4217Namespace, "USD"), EName(XbrliNamespace, "pure"), EName(XbrliNamespace, "shares"))) {
      measureNames
    }
  }

  it should "support finding the first descendant element obeying some property" in {
    // Semantic query: Find the first optional XBRL context with entity identifier "1234567890"
    // using scheme "http://www.sec.gov/CIK".

    // Yaidom query: Find the first optional descendant element of the root element that is an xbrli:context
    // having an entity identifier for scheme "http://www.sec.gov/CIK" having value "1234567890".
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following function

    def isContextHavingEntity(elem: BackingElemApi, scheme: String, identifier: String): Boolean = ???

    // Method findElem finds the optional first descendant element obeying the given element predicate;
    // the word "descendant" is implicit in the name.
    // An element predicate ("filter") is passed as argument.
    // Like the name says, only element nodes are returned.

    val interestingContextOption: Option[BackingElemApi] =
      rootElem.findElem(e => isContextHavingEntity(e, "http://www.sec.gov/CIK", "1234567890"))

    assertResult(Some(EName(XbrliNamespace, "context"))) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("I-2007")) {
      // This would fail if the context had no ID attribute.
      // So, using method attributeOption instead of attribute is more robust,
      // but here we use the knowledge that a context must have an ID attribute.

      interestingContextOption.map(_.attribute(IdEName))
    }
  }

  it should "support finding the first descendant element obeying some property about QName-valued attributes" in {
    // Semantic query: Find the first optional XBRL context with dimension gaap:ClassOfPreferredStockDescriptionAxis
    // (as the corresponding EName) in its segment.

    // Yaidom query: Find the first optional descendant element of the root element that is an xbrli:context
    // having a segment containing an explicit member with dimension gaap:ClassOfPreferredStockDescriptionAxis (as EName).
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following variable

    val interestingContextOption: Option[BackingElemApi] = ???

    assertResult(Some(EName(XbrliNamespace, "context"))) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("D-2007-PSA")) {
      // This would fail if the context had no ID attribute.
      // So, using method attributeOption instead of attribute is more robust,
      // but here we use the knowledge that a context must have an ID attribute.

      interestingContextOption.map(_.attribute(IdEName))
    }
  }

  it should "support querying QName-valued attributes and texts" in {
    // Semantic query: Find all dimensions and their members occurring in the XBRL instance.

    // Yaidom query: Find all explicit member descendant elements of the root element, and
    // build a Map from dimension ENames to Sets of member ENames from those explicit members.

    // Implement the following variable

    val dimensionMembers: Map[EName, Set[EName]] = ???

    val scope = Scope.from("gaap" -> GaapNamespace)
    import scope._

    assertResult(true) {
      Set(QName("gaap:EntityAxis").res, QName("gaap:VerificationAxis").res, QName("gaap:ReportDateAxis").res).
        subsetOf(dimensionMembers.keySet)
    }
    assertResult(true) {
      Set(QName("gaap:ABCCompanyDomain").res).subsetOf(dimensionMembers(QName("gaap:EntityAxis").res))
    }
    assertResult(true) {
      Set(QName("gaap:UnqualifiedOpinionMember").res).subsetOf(dimensionMembers(QName("gaap:VerificationAxis").res))
    }
  }

  it should "support querying ancestor elements" in {
    // Semantic query: Find all XBRL contexts for (instant) period 2006-12-31.

    // Yaidom query: Find all 2006-12-31 (instant) periods, and return their ancestor XBRL contexts.

    // Implement the following variable

    val interestingPeriods: immutable.IndexedSeq[BackingElemApi] = ???

    def isContext(elem: BackingElemApi): Boolean = elem.resolvedName == EName(XbrliNamespace, "context")

    // Method findAncestor finds the optional ancestor element obeying the given element predicate.
    // An element predicate ("filter") is passed as argument.

    val interestingContexts: immutable.IndexedSeq[BackingElemApi] =
      interestingPeriods.flatMap(e => e.findAncestor(isContext))

    assertResult(Set("I-2006")) {
      interestingContexts.map(_.attribute(IdEName).take(6)).toSet
    }

    // We could also write the same query in a top-down manner instead of a bottom-up manner, like shown below.
    // The bottom-up versions seems less verbose, however.

    val expectedInterestingContexts: immutable.IndexedSeq[BackingElemApi] =
      rootElem filterElems { e =>
        e.resolvedName == EName(XbrliNamespace, "context") &&
          e.filterElems(withEName(XbrliNamespace, "instant")).exists(_.text == "2006-12-31")
      }

    assertResult(expectedInterestingContexts) {
      interestingContexts
    }
  }

  it should "support non-trivial queries involving ancestor elements" in {
    // Semantic query: Find all facts in the instance.

    // Yaidom query: Find all descendant elements of the root element that are not in the xbrli or link namespaces
    // and that have no ancestors in those namespaces other than the xbrli:xbrl root element.

    // Implement the following variable

    val facts: immutable.IndexedSeq[BackingElemApi] = ???

    assertResult(Set(GaapNamespace)) {
      facts.flatMap(_.resolvedName.namespaceUriOption).toSet
    }
    assertResult(Vector()) {
      facts.filter(_.resolvedName.namespaceUriOption.isEmpty)
    }
  }
}
