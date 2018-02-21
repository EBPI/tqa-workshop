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
import eu.cdevreeze.tqa.backingelem.nodeinfo.docbuilder.SaxonDocumentBuilder
import eu.cdevreeze.tqa.docbuilder.jvm.UriConverters
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
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
 * Study the input file (sample-Instance-Proof.xml) as well, because the test methods use this input.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class QuerySpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi" element with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)
  private val docBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromUriConverter(UriConverters.identity))

  private val rootElem: BackingElemApi =
    docBuilder.build(classOf[QuerySpec].getResource("/sample-Instance-Proof.xml").toURI).documentElement

  private val XbrldiNamespace = "http://xbrl.org/2006/xbrldi"
  private val Iso4217Namespace = "http://www.xbrl.org/2003/iso4217"

  private val GaapNamespace = "http://xasb.org/gaap"

  // Some EName constants that can be used below

  private val XbrliXbrlEName = EName(XbrliNamespace, "xbrl")
  private val XbrliContextEName = EName(XbrliNamespace, "context")
  private val XbrliUnitEName = EName(XbrliNamespace, "unit")
  private val XbrliPeriodEName = EName(XbrliNamespace, "period")
  private val XbrliInstantEName = EName(XbrliNamespace, "instant")
  private val XbrliEntityEName = EName(XbrliNamespace, "entity")
  private val XbrliIdentifierEName = EName(XbrliNamespace, "identifier")
  private val XbrliSegmentEName = EName(XbrliNamespace, "segment")
  private val XbrliMeasureEName = EName(XbrliNamespace, "measure")

  private val XbrldiExplicitMemberEName = EName(XbrldiNamespace, "explicitMember")

  // Note that we could also have written EName(Some(XbrldiNamespace), "explicitMember") or
  // EName("{http://xbrl.org/2006/xbrldi}explicitMember"). They would all create the same EName.

  // In the tests below, use ENames and not (lexical) QNames in queries and element predicates.
  // Use the EName constants above where applicable, and feel free to create EName constants where needed.

  //
  // Exercise 1
  //

  "The query API" should "support filtering of child elements" in {
    // Semantic query: Find all XBRL contexts whose ID starts with the string "I-2007".

    // Yaidom query: Filter all child elements of the root element named xbrli:context, having an ID attribute
    // starting with string "I-2007".

    def hasIdStartingWithI2007(elem: BackingElemApi): Boolean = {
      elem.attributeOption(IdEName).exists(_.startsWith("I-2007"))
    }

    // Implement the following function, using the EName corresponding to QName xbrli:context to test the element name

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

  //
  // Exercise 2
  //

  it should "support filtering of descendant elements" in {
    // Semantic query: Find all explicit members in XBRL contexts.

    // Yaidom query: Filter all descendant elements of the root element named xbrldi:explicitMember.

    // Implement the following function, using the EName corresponding to QName xbrldi:explicitMember to test the element name

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

  //
  // Exercise 3
  //

  it should "support filtering of descendant-or-self elements" in {
    // Semantic query: Find all elements in the xbrli namespace.

    // Yaidom query: Filter all descendant-or-self elements of the root element in the xbrli namespace.

    // Implement the following function, using the namespace corresponding to prefix "xbrli" to test the element's namespace

    def isInXbrliNamespace(elem: BackingElemApi): Boolean = ???

    // Method filterElemsOrSelf filters descendant-or-self elements; the word "descendant" is implicit in the name.
    // An element predicate ("filter") is passed as argument.
    // Like the name says, only element nodes are returned.

    val xbrliElems: immutable.IndexedSeq[BackingElemApi] =
      rootElem.filterElemsOrSelf(isInXbrliNamespace)

    assertResult(true) {
      val xbrliENames = xbrliElems.map(_.resolvedName).toSet

      val someXbrliENames: Set[EName] =
        Set(XbrliXbrlEName, XbrliContextEName, XbrliEntityEName, XbrliIdentifierEName, XbrliSegmentEName, XbrliPeriodEName, XbrliUnitEName)

      someXbrliENames.subsetOf(xbrliENames)
    }

    assertResult(true) {
      xbrliElems.filter(_.resolvedName.namespaceUriOption.contains(GaapNamespace)).isEmpty
    }
  }

  //
  // Exercise 4
  //

  it should "support retrieval of attributes" in {
    // Semantic query: Find all XBRL unit IDs.

    // Yaidom query: Find all xbrli:unit element ID attributes.
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following variable. Find all xbrli:unit elements, and return their id attribute values.

    val unitIds: Set[String] = ???

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitIds
    }
  }

  //
  // Exercise 5
  //

  it should "support retrieval of optional attributes" in {
    // Semantic query: Find all numeric item fact unitRefs.

    // Yaidom query: Find all unitRef attributes in descendant elements of the root element.
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following variable. Find all descendant elements, and return their optional unitRef attributes.

    val unitRefs: Set[String] = ???

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitRefs
    }
  }

  //
  // Exercise 6
  //

  it should "support retrieval of element texts" in {
    // Semantic query: Find all gaap:RelatedPartyTypeOfRelationship fact values.

    // Yaidom query: Find all texts of descendant elements of the root element that have
    // element name gaap:RelatedPartyTypeOfRelationship.

    // Implement the following variable. Find all gaap:RelatedPartyTypeOfRelationship elements, and return their element texts.

    val interestingFactValues: Set[String] = ???

    assertResult(Set("Parent", "JointVenture")) {
      interestingFactValues
    }
  }

  //
  // Exercise 7
  //

  it should "support retrieval of QName-valued texts" in {
    // Semantic query: Find all measures (as expanded names).

    // Yaidom query: Find all texts as ENames of descendant elements of the root element that have
    // element name xbrli:measure.

    // Implement the following variable. Find all xbrli:measure elements, and return their element texts resolved as ENames.
    // For some background about QName-valued element texts, see the Evan Lenz article on Understanding XML Namespaces.

    val measureNames: Set[EName] = ???

    assertResult(Set(EName(Iso4217Namespace, "USD"), EName(XbrliNamespace, "pure"), EName(XbrliNamespace, "shares"))) {
      measureNames
    }
  }

  //
  // Exercise 8
  //

  it should "support finding the first descendant element obeying some property" in {
    // Semantic query: Find the first optional XBRL context with entity identifier "1234567890"
    // using scheme "http://www.sec.gov/CIK".

    // Yaidom query: Find the first optional descendant element of the root element that is an xbrli:context
    // having an entity identifier for scheme "http://www.sec.gov/CIK" having value "1234567890".
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following function. See above, but here the scheme and identifier are parameters. This is a more challenging exercise.

    def isContextHavingEntityIdentifier(elem: BackingElemApi, scheme: String, identifier: String): Boolean = ???

    // Method findElem finds the optional first descendant element obeying the given element predicate;
    // the word "descendant" is implicit in the name.
    // An element predicate ("filter") is passed as argument.
    // Like the name says, only element nodes are returned.

    val interestingContextOption: Option[BackingElemApi] =
      rootElem.findElem(e => isContextHavingEntityIdentifier(e, "http://www.sec.gov/CIK", "1234567890"))

    assertResult(Some(XbrliContextEName)) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("I-2007")) {
      // This would fail if the context had no ID attribute.
      // So, using method attributeOption instead of attribute is more robust,
      // but here we use the knowledge that a context must have an ID attribute.

      interestingContextOption.map(_.attribute(IdEName))
    }
  }

  //
  // Exercise 9
  //

  it should "support finding the first descendant element obeying some property about QName-valued attributes" in {
    // Semantic query: Find the first optional XBRL context with dimension gaap:ClassOfPreferredStockDescriptionAxis
    // (as the corresponding EName) in its segment.

    // Yaidom query: Find the first optional descendant element of the root element that is an xbrli:context
    // having a segment containing an explicit member with dimension gaap:ClassOfPreferredStockDescriptionAxis (as EName).
    // This query contains several yaidom query API calls, instead of just one query API call.

    // Implement the following variable. See above for the xbrli:context searched for. This is a more challenging exercise.
    // For some background about QName-valued attribute values, see the Evan Lenz article on Understanding XML Namespaces.

    val interestingContextOption: Option[BackingElemApi] = ???

    assertResult(Some(XbrliContextEName)) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("D-2007-PSA")) {
      // This would fail if the context had no ID attribute.
      // So, using method attributeOption instead of attribute is more robust,
      // but here we use the knowledge that a context must have an ID attribute.

      interestingContextOption.map(_.attribute(IdEName))
    }
  }

  //
  // Exercise 10
  //

  it should "support querying QName-valued attributes and texts" in {
    // Semantic query: Find all dimensions and their members occurring in the XBRL instance.

    // Yaidom query: Find all explicit member descendant elements of the root element, and
    // build a Map from dimension ENames to Sets of member ENames from those explicit members.

    // Implement the following variable. Challenging. Hint: first find all desired elements, and next group them on the dimension.
    // A Scala collection can be grouped using method groupBy. Method mapValues may also come in handy.

    val dimensionMembers: Map[EName, Set[EName]] = ???

    val scope = Scope.from("gaap" -> GaapNamespace)
    import scope._

    // Note how we create ENames from QNames using an implicit conversion. This is yet another way of creating ENames.

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

  //
  // Exercise 11
  //

  it should "support querying ancestor elements" in {
    // Semantic query: Find all XBRL contexts for (instant) period 2006-12-31.

    // Yaidom query: Find all 2006-12-31 (instant) periods, and return their ancestor XBRL contexts.

    // Implement the following variable. Somewhat challenging, but less so after the preceding exercises.
    // The elements returned must be xbrli:period elements, containing an xbrli:instant element for 2006-12-31.

    val interestingPeriods: immutable.IndexedSeq[BackingElemApi] = ???

    def isContext(elem: BackingElemApi): Boolean = elem.resolvedName == XbrliContextEName

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
        e.resolvedName == XbrliContextEName &&
          e.filterElems(withEName(XbrliInstantEName)).exists(_.text == "2006-12-31")
      }

    assertResult(expectedInterestingContexts) {
      interestingContexts
    }
  }

  //
  // Exercise 12
  //

  it should "support non-trivial queries involving ancestor elements" in {
    // Semantic query: Find all facts in the instance.

    // Yaidom query: Find all descendant elements of the root element that are not in the xbrli or link namespaces
    // and that have no ancestors in those namespaces other than the xbrli:xbrl root element.

    // Implement the following variable. See above for which elements must be returned. This is a rather challenging exercise.

    val facts: immutable.IndexedSeq[BackingElemApi] = ???

    assertResult(Set(GaapNamespace)) {
      facts.flatMap(_.resolvedName.namespaceUriOption).toSet
    }
    assertResult(Vector()) {
      facts.filter(_.resolvedName.namespaceUriOption.isEmpty)
    }
  }
}
