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

package eu.cdevreeze.tqaworkshop.chapter3

import java.time.LocalDate

import scala.collection.immutable
import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames.IdEName
import eu.cdevreeze.tqa.Namespaces.XbrliNamespace
import eu.cdevreeze.tqa.docbuilder.saxon.SaxonDocumentBuilder
import eu.cdevreeze.tqa.docbuilder.jvm.UriConverters
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.tqaworkshop.xbrlinstance.ExplicitMember
import eu.cdevreeze.tqaworkshop.xbrlinstance.Fact
import eu.cdevreeze.tqaworkshop.xbrlinstance.Instant
import eu.cdevreeze.tqaworkshop.xbrlinstance.InstantPeriod
import eu.cdevreeze.tqaworkshop.xbrlinstance.Period
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrliContext
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrliElem
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import net.sf.saxon.s9api.Processor

/**
 * Test specification for "type-safe" yaidom querying of XBRL instances. The abstraction level is higher
 * than low level untyped XML.
 *
 * This test is very similar to the equally named test in chapter 1, except that the abstraction level
 * is higher. Hence, we need less code than in the same test in chapter 1, and the code is more type-safe.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the yaidom library available.
 * Study the `XbliElem` class and its sub-types in this package.
 *
 * Study the input file (sample-Instance-Proof.xml) as well, because the test methods use this input.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class QuerySpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi-backed" XbrlInstance with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)
  private val docBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromUriConverter(UriConverters.identity))

  private val rootElem: XbrlInstance = {
    val doc = docBuilder.build(classOf[QuerySpec].getResource("/sample-Instance-Proof.xml").toURI)
    XbrlInstance(doc.documentElement)
  }

  private val XbrldiNamespace = "http://xbrl.org/2006/xbrldi"
  private val Iso4217Namespace = "http://www.xbrl.org/2003/iso4217"

  private val GaapNamespace = "http://xasb.org/gaap"

  // In the tests below, do not use any (lexical) QNames.
  // Due to the type-safe XBRL instance "yaidom dialect", ENames also rarely occur in the code.

  //
  // Exercise 1
  //

  "The XBRL instance (\"yaidom dialect\") query API" should "support filtering XbrliContext elements" in {
    // Semantic query: Find all XBRL contexts whose ID starts with the string "I-2007".

    def isContextHavingIdStartingWithI2007(context: XbrliContext): Boolean = {
      context.id.startsWith("I-2007")
    }

    // Class XbrlInstance directly offers a method to filter contexts.

    // Implement the following variable, filtering all XbrliContext elements obeying the predicate above.

    val filteredContexts: immutable.IndexedSeq[XbrliContext] = {
      ???
    }

    assertResult(26) {
      filteredContexts.size
    }

    // We can also use the type-safe variant of yaidom method filterChildElems!

    assertResult(rootElem.filterChildElemsOfType(classTag[XbrliContext])(isContextHavingIdStartingWithI2007)) {
      filteredContexts
    }
  }

  //
  // Exercise 2
  //

  it should "support filtering of descendant elements such as ExplicitMember elements" in {
    // Semantic query: Find all explicit members in XBRL contexts.

    // Use a "type-safe yaidom query method" to find all explicit member elements

    // Implement the following variable, finding all ExplicitMember elements

    val explicitMembers: immutable.IndexedSeq[ExplicitMember] = {
      ???
    }

    assertResult(Set("explicitMember")) {
      explicitMembers.map(_.localName).toSet
    }

    assertResult(true) {
      val dimensions: Set[EName] = explicitMembers.map(_.dimension).toSet

      val someDimensions: Set[EName] =
        List("EntityAxis", "BusinessSegmentAxis", "VerificationAxis", "PremiseAxis", "ReportDateAxis").
          map(localName => EName(GaapNamespace, localName)).toSet

      someDimensions.subsetOf(dimensions)
    }
  }

  //
  // Exercise 3
  //

  it should "support filtering of descendant-or-self elements such as all XbrliElem elements in the xbrli namespace" in {
    // Semantic query: Find all elements in the xbrli namespace.

    // Implement the following function, returning true if the element is in the namespace for prefix xbrli

    def isInXbrliNamespace(elem: XbrliElem): Boolean = {
      ???
    }

    // Look, a regular ("non-type-safe") yaidom query method. Still, the results are XbrliElem objects.

    val xbrliElems: immutable.IndexedSeq[XbrliElem] =
      rootElem.filterElemsOrSelf(isInXbrliNamespace)

    assertResult(true) {
      val xbrliENames = xbrliElems.map(_.resolvedName).toSet

      import XbrliElem._

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

  it should "support retrieval of attributes such as ID attributes" in {
    // Semantic query: Find all XBRL unit IDs.

    // It is easy to query for units and their IDs.

    // Implement the following variable. Find all units, and their ID properties.

    val unitIds: Set[String] = {
      ???
    }

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitIds
    }
  }

  //
  // Exercise 5
  //

  it should "support retrieval of optional attributes such as unitRef attributes for items" in {
    // Semantic query: Find all numeric item fact unitRefs.

    // Implement the following variable. Find all item facts, and return their optional unitRef attributes.

    val unitRefs: Set[String] = {
      ???
    }

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitRefs
    }
  }

  //
  // Exercise 6
  //

  it should "support retrieval of element texts such as fact values" in {
    // Semantic query: Find all gaap:RelatedPartyTypeOfRelationship fact values.

    // Implement the following variable. Find all gaap:RelatedPartyTypeOfRelationship item facts, and return their element texts.

    val interestingFactValues: Set[String] = {
      ???
    }

    assertResult(Set("Parent", "JointVenture")) {
      interestingFactValues
    }
  }

  //
  // Exercise 7
  //

  it should "support retrieval of QName-valued texts such as unit measures" in {
    // Semantic query: Find all measures (as expanded names).

    // Implement the following variable. Find all xbrli:measure elements, and return their element texts resolved as ENames.
    // If there is no specific class in the instance model for xbrli:measure elements, then the measure will be of type XbrliElem.
    // In that case, not much is gained for measure elements compared to the use of raw yaidom.

    val measureNames: Set[EName] = {
      ???
    }

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

    // Implement the following function. See above, but here the scheme and identifier are parameters.
    // This exercise is a lot easier than the equivalent one using raw yaidom.

    def hasEntityIdentifier(elem: XbrliContext, scheme: String, identifier: String): Boolean = {
      ???
    }

    val interestingContextOption: Option[XbrliContext] =
      rootElem.findElemOfType(classTag[XbrliContext])(e => hasEntityIdentifier(e, "http://www.sec.gov/CIK", "1234567890"))

    assertResult(Some(XbrliElem.XbrliContextEName)) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("I-2007")) {
      // This would fail if the context had no ID attribute.
      // Yet here we use the knowledge that a context must have an ID.

      interestingContextOption.map(_.attribute(IdEName))
    }
  }

  //
  // Exercise 9
  //

  it should "support finding the first descendant element obeying some property about QName-valued attributes such as dimension attributes" in {
    // Semantic query: Find the first optional XBRL context with dimension gaap:ClassOfPreferredStockDescriptionAxis
    // (as the corresponding EName) in its segment.

    // Implement the following variable. See above for the XbrliContext searched for.

    val interestingContextOption: Option[XbrliContext] = {
      ???
    }

    assertResult(Some(XbrliElem.XbrliContextEName)) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("D-2007-PSA")) {
      // This would fail if the context had no ID attribute.
      // Yet here we use the knowledge that a context must have an ID.

      interestingContextOption.map(_.id)
    }
  }

  //
  // Exercise 10
  //

  it should "support querying QName-valued attributes and texts such as explicit member element dimensions and members" in {
    // Semantic query: Find all dimensions and their members occurring in the XBRL instance.

    // Implement the following variable. Challenging. Hint: first find all ExplicitMember elements, and next group them on the dimension.
    // A Scala collection can be grouped using method groupBy. Method mapValues may also come in handy.

    val dimensionMembers: Map[EName, Set[EName]] = {
      ???
    }

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

  //
  // Exercise 11
  //

  it should "support querying ancestor elements such as surrounding contexts from periods" in {
    // Semantic query: Find all XBRL contexts for (instant) period 2006-12-31.

    // Implement the following variable. Somewhat challenging, but less so after the preceding exercises.
    // The elements returned must be Period elements, containing an Instant element for 2006-12-31.

    val interestingPeriods: immutable.IndexedSeq[Period] = {
      ???
    }

    val interestingContexts: immutable.IndexedSeq[XbrliContext] = {
      // Going up instead of going down is clumsy, uses low level yaidom, and may be expensive at runtime

      val contextElems: immutable.IndexedSeq[BackingElemApi] =
        interestingPeriods.map(_.backingElem).flatMap(_.findAncestor(withEName(XbrliElem.XbrliContextEName)))

      // This is the potentially expensive part: recursively creating type-safe DOM trees (here for contexts)

      contextElems.map(e => XbrliElem(e).asInstanceOf[XbrliContext])
    }

    assertResult(Set("I-2006")) {
      interestingContexts.map(_.attribute(IdEName).take(6)).toSet
    }

    // We could also write the same query in a top-down manner instead of a bottom-up manner, like shown below.
    // The bottom-up versions seems less verbose, however.

    val expectedInterestingContexts: immutable.IndexedSeq[XbrliContext] =
      rootElem filterContexts { e =>
        e.period.findElemOfType(classTag[Instant])(_.dateTime == LocalDate.parse("2006-12-31").atStartOfDay.plusDays(1)).nonEmpty
      }

    assertResult(expectedInterestingContexts) {
      interestingContexts
    }
  }

  //
  // Exercise 12
  //

  it should "support Fact queries" in {
    // Semantic query: Find all facts in the instance.

    // Implement the following variable by finding all facts (trivial, unlike the corresponding low level yaidom code)

    val facts: immutable.IndexedSeq[Fact] = {
      ???
    }

    assertResult(Set(GaapNamespace)) {
      facts.flatMap(_.resolvedName.namespaceUriOption).toSet
    }
    assertResult(Vector()) {
      facts.filter(_.resolvedName.namespaceUriOption.isEmpty)
    }
  }
}
