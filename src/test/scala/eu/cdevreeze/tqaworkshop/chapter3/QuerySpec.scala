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
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
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
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class QuerySpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi-backed" XbrlInstance with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)
  private val docBuilder = new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uri))

  private val rootElem: XbrlInstance = {
    val elem = docBuilder.build(classOf[QuerySpec].getResource("/sample-Instance-Proof.xml").toURI)
    XbrlInstance(elem)
  }

  private val XbrldiNamespace = "http://xbrl.org/2006/xbrldi"
  private val Iso4217Namespace = "http://www.xbrl.org/2003/iso4217"

  private val GaapNamespace = "http://xasb.org/gaap"

  // In the tests below, do not use any (lexical) QNames.
  // Due to the type-safe XBRL instance "yaidom dialect", ENames also rarely occur in the code.

  "The query API" should "support filtering of child elements such as xbrli:context elements" in {
    // Semantic query: Find all XBRL contexts whose ID starts with the string "I-2007".

    def isContextHavingIdStartingWithI2007(context: XbrliContext): Boolean = {
      context.id.startsWith("I-2007")
    }

    // Class XbrlInstance directly offers a method to filter contexts.

    // Implement the following variable

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

  it should "support filtering of descendant elements such as xbrldi:explicitMember elements" in {
    // Semantic query: Find all explicit members in XBRL contexts.

    // Use a "type-safe yaidom query method" to find all explicit member elements

    // Implement the following variable

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

  it should "support filtering of descendant-or-self elements such as all instance elements in the xbrli namespace" in {
    // Semantic query: Find all elements in the xbrli namespace.

    // Implement the following function

    def isInXbrliNamespace(elem: XbrliElem): Boolean = {
      ???
    }

    // Look, a regular ("non-type-safe") yaidom query method. Still, the results are XbrliElem objects.

    val xbrliElems: immutable.IndexedSeq[XbrliElem] =
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

  it should "support retrieval of attributes such as ID attributes" in {
    // Semantic query: Find all XBRL unit IDs.

    // It is easy to query for units and their IDs.

    // Implement the following variable

    val unitIds: Set[String] = {
      ???
    }

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitIds
    }
  }

  it should "support retrieval of optional attributes such as unitRef attributes for items" in {
    // Semantic query: Find all numeric item fact unitRefs.

    // Implement the following variable

    val unitRefs: Set[String] = {
      ???
    }

    assertResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      unitRefs
    }
  }

  it should "support retrieval of element texts such as fact values" in {
    // Semantic query: Find all gaap:RelatedPartyTypeOfRelationship fact values.

    // Implement the following variable

    val interestingFactValues: Set[String] = {
      ???
    }

    assertResult(Set("Parent", "JointVenture")) {
      interestingFactValues
    }
  }

  it should "support retrieval of QName-valued texts such as unit measures" in {
    // Semantic query: Find all measures (as expanded names).

    // Implement the following variable

    val measureNames: Set[EName] = {
      ???
    }

    assertResult(Set(EName(Iso4217Namespace, "USD"), EName(XbrliNamespace, "pure"), EName(XbrliNamespace, "shares"))) {
      measureNames
    }
  }

  it should "support finding the first descendant element obeying some property" in {
    // Semantic query: Find the first optional XBRL context with entity identifier "1234567890"
    // using scheme "http://www.sec.gov/CIK".

    // Implement the following function

    def hasEntity(elem: XbrliContext, scheme: String, identifier: String): Boolean = {
      ???
    }

    val interestingContextOption: Option[XbrliContext] =
      rootElem.findElemOfType(classTag[XbrliContext])(e => hasEntity(e, "http://www.sec.gov/CIK", "1234567890"))

    assertResult(Some(EName(XbrliNamespace, "context"))) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("I-2007")) {
      // This would fail if the context had no ID attribute.
      // Yet here we use the knowledge that a context must have an ID.

      interestingContextOption.map(_.attribute(IdEName))
    }
  }

  it should "support finding the first descendant element obeying some property about QName-valued attributes such as dimension attributes" in {
    // Semantic query: Find the first optional XBRL context with dimension gaap:ClassOfPreferredStockDescriptionAxis
    // (as the corresponding EName) in its segment.

    // Implement the following variable

    val interestingContextOption: Option[XbrliContext] = {
      ???
    }

    assertResult(Some(EName(XbrliNamespace, "context"))) {
      interestingContextOption.map(_.resolvedName)
    }

    assertResult(Some("D-2007-PSA")) {
      // This would fail if the context had no ID attribute.
      // Yet here we use the knowledge that a context must have an ID.

      interestingContextOption.map(_.id)
    }
  }

  it should "support querying QName-valued attributes and texts such as explicit member element dimensions and members" in {
    // Semantic query: Find all dimensions and their members occurring in the XBRL instance.

    // Implement the following variable

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

  it should "support querying ancestor elements such as surrounding contexts from periods" in {
    // Semantic query: Find all XBRL contexts for (instant) period 2016-12-31.

    // Implement the following variable

    val interestingPeriods: immutable.IndexedSeq[Period] = {
      ???
    }

    val interestingContexts: immutable.IndexedSeq[XbrliContext] = {
      // Going up instead of going down is clumsy, uses low level yaidom, and may be expensive at runtime

      val contextElems: immutable.IndexedSeq[BackingElemApi] =
        interestingPeriods.map(_.backingElem).flatMap(_.findAncestor(withEName(XbrliNamespace, "context")))

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

  it should "support queries (explicitly or implicitly) involving ancestor elements such as fact queries" in {
    // Semantic query: Find all facts in the instance.

    // Implement the following variable (trivial, unlike the corresponding low level yaidom code)

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
