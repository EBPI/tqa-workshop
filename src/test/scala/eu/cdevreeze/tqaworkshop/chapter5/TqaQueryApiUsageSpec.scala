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

package eu.cdevreeze.tqaworkshop.chapter5

import java.io.File
import java.net.URI

import scala.collection.immutable
import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.Namespaces
import eu.cdevreeze.tqa.backingelem.nodeinfo.docbuilder.SaxonDocumentBuilder
import eu.cdevreeze.tqa.base.dom.ConceptLabelResource
import eu.cdevreeze.tqa.base.dom.ConceptReferenceResource
import eu.cdevreeze.tqa.base.dom.RoleType
import eu.cdevreeze.tqa.base.dom.XLinkResource
import eu.cdevreeze.tqa.base.queryapi.ParentChildRelationshipPath
import eu.cdevreeze.tqa.base.relationship.ConceptLabelRelationship
import eu.cdevreeze.tqa.base.relationship.ConceptReferenceRelationship
import eu.cdevreeze.tqa.base.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.base.relationship.DefinitionRelationship
import eu.cdevreeze.tqa.base.relationship.ElementLabelRelationship
import eu.cdevreeze.tqa.base.relationship.OtherNonStandardRelationship
import eu.cdevreeze.tqa.base.relationship.ParentChildRelationship
import eu.cdevreeze.tqa.base.relationship.StandardRelationship
import eu.cdevreeze.tqa.base.taxonomy.BasicTaxonomy
import eu.cdevreeze.tqa.base.taxonomybuilder.DefaultDtsCollector
import eu.cdevreeze.tqa.base.taxonomybuilder.TaxonomyBuilder
import eu.cdevreeze.tqa.docbuilder.jvm.UriConverters
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor

/**
 * Test specification for using the TQA query API, querying for relationships and taxonomy DOM elements.
 *
 * Before doing this exercise, make sure to have done all exercises of the preceding chapters. This exercise is about
 * 2 things: the notion of relationships (as opposed to lower level XLink arcs), and the use of the TQA query API
 * for finding taxonomy content (relationships and taxonomy DOM elements) in general. This exercise explores the use of TQA
 * in practice, and may therefore be the most important exercise of the TQA workshop.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class TqaQueryApiUsageSpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi-backed" XbrlInstance with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)

  private val instanceDocBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromUriConverter(UriConverters.identity))

  private val xbrlInstance: XbrlInstance = {
    val doc = instanceDocBuilder.build(classOf[TqaQueryApiUsageSpec].getResource("/kvk-rpt-jaarverantwoording-2016-nlgaap-klein-publicatiestukken.xbrl").toURI)
    XbrlInstance(doc.documentElement)
  }

  private val taxo: BasicTaxonomy = {
    // Like in the chapter 4 SchemaUsageSpec exercise, we create a BasicTaxonomy.
    // Unlike that exercise, here we use a TaxonomyBuilder, and we build a "closed DTS" for one entrypoint schema.
    // See the Core XBRL specification for the notion of a DTS (discoverable taxonomy set) and
    // the rules to gather an entire DTS. Due to the creation of a DTS, we do not need to provide any external knowledge
    // about substitution groups.

    val rootDir = new File(classOf[TqaQueryApiUsageSpec].getResource("/taxonomy").toURI)

    val taxoDocBuilder =
      new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromLocalMirrorRootDirectory(rootDir))

    val entrypointUri =
      URI.create("http://www.nltaxonomie.nl/nt11/kvk/20161214/entrypoints/kvk-rpt-jaarverantwoording-2016-nlgaap-klein-publicatiestukken.xsd")

    // The DocumentCollector knows which taxonomy documents belong to the resulting BasicTaxonomy.
    // Here the DocumentCollector is a DefaultDtsCollector, which collects the documents belonging to the DTS.

    val documentCollector = DefaultDtsCollector()

    // The "relationship factory" is used for turning XLink arcs into (higher level) relationships.
    // Without it, we would not be able to query the taxonomy for relationships.

    val relationshipFactory = DefaultRelationshipFactory.StrictInstance

    val taxoBuilder =
      TaxonomyBuilder.
        withDocumentBuilder(taxoDocBuilder).
        withDocumentCollector(documentCollector).
        withRelationshipFactory(relationshipFactory)

    // Use the TaxonomyBuilder to create the BasicTaxonomy. It offers the TQA taxonomy query API,
    // whose functions mainly return taxonomy content and relationships.

    taxoBuilder.build(Set(entrypointUri))
  } ensuring (taxo => taxo.relationships.nonEmpty)

  private val RjiNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data"
  private val RjtNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-tuples"
  private val RjdmNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-domains"
  private val VenjBw2iNamespace = "http://www.nltaxonomie.nl/nt11/venj/20161214/dictionary/venj-bw2-data"
  private val VenjBw2AbstrNamespace = "http://www.nltaxonomie.nl/nt11/venj/20161214/presentation/venj-bw2-abstracts"
  private val KvkAbstrNamespace = "http://www.nltaxonomie.nl/nt11/kvk/20161214/presentation/kvk-abstracts"

  //
  // Exercise 1
  //

  "The TQA query API" should "support retrieval of concept labels" in {
    // For all facts in the sample XBRL instance, find their (item and tuple) concepts in the taxonomy, and their
    // verbose Dutch labels (falling back to standard labels if the verbose label does not exist).

    // Note that in XBRL there are 2 entirely different "label" terms. One "label" term refers to XLink labels, and
    // the "label" term used here refers to label texts associated with concepts in so-called label links.

    val verboseLabelRole = "http://www.xbrl.org/2003/role/verboseLabel"
    val standardLabelRole = "http://www.xbrl.org/2003/role/label"

    // Implement getting the verbose and otherwise standard Dutch label of the given concept. Not trivial, but
    // many hints are given.

    // To find a concept label, we have to find a ConceptLabelRelationship, and return its ConceptLabelResource.
    // There is no need to descend to the level of XLink arcs, locators and resources. Just query for concept-label
    // relationships and their label resources.

    def findConceptLabelResource(concept: EName): Option[ConceptLabelResource] = {
      // Use the taxo to find the (first) desired ConceptLabelResource, if any

      // Filter all concept-label relationships outgoing from the concept having language "nl".
      // Use query API method filterOutgoingConceptLabelRelationships to that end.

      val conceptLabelRelationships: immutable.IndexedSeq[ConceptLabelRelationship] =
        ???

      // In this collection of concept-label relationships, find the first one having
      // the resource role "http://www.xbrl.org/2003/role/verboseLabel", falling back
      // to "http://www.xbrl.org/2003/role/label" if there is no verbose Dutch label.

      val conceptLabelRelationshipOption: Option[ConceptLabelRelationship] =
        ???

      // Return the optional label resource of the optional concept-label relationship

      conceptLabelRelationshipOption.map(rel => ???)
    }

    // First find the (item and tuple) concepts for all facts in the instance.
    // The EName of a fact identifies its concept in the taxonomy.

    val concepts: Set[EName] = xbrlInstance.findAllFacts.map(_.resolvedName).toSet

    val conceptLabelTexts: Map[EName, String] =
      concepts.toSeq.flatMap(concept => findConceptLabelResource(concept).map(res => (concept -> res))).
        toMap.mapValues(_.trimmedText)

    assertResult(Some("Som van de herwaarderingen van materiële vaste activa op de balansdatum")) {
      // Verbose label
      conceptLabelTexts.get(EName(VenjBw2iNamespace, "PropertyPlantEquipmentAccumulatedRevaluations"))
    }

    assertResult(Some("Type grondslag")) {
      // Standard label, because verbose Dutch label is missing
      conceptLabelTexts.get(EName(VenjBw2iNamespace, "BasisOfPreparation"))
    }
  }

  //
  // Exercise 2
  //

  it should "support retrieval of concept references" in {
    // For all facts in the sample XBRL instance, find their (item and tuple) concepts in the taxonomy, and their
    // standard references.

    val standardReferenceRole = "http://www.xbrl.org/2003/role/reference"

    // Implement getting the standard reference of the given concept. Not trivial, but after exercise 1 not too hard either.
    // The implementation approach is very similar to that of exercise 1.

    // To find a concept reference, we have to find a ConceptReferenceRelationship, and return its ConceptReferenceResource.
    // There is no need to descend to the level of XLink arcs, locators and resources. Just query for concept-reference
    // relationships and their reference resources.

    def findConceptReferenceResource(concept: EName): Option[ConceptReferenceResource] = {
      // Use the taxo to find the (first) standard ConceptReferenceResource for the given concept, if any

      val conceptReferenceRelationshipOption: Option[ConceptReferenceRelationship] =
        ???

      conceptReferenceRelationshipOption.map(rel => ???)
    }

    // First find the (item and tuple) concepts for all facts in the instance.
    // The EName of a fact identifies its concept in the taxonomy.

    val concepts: Set[EName] = xbrlInstance.findAllFacts.map(_.resolvedName).toSet

    val conceptReferences: Map[EName, ConceptReferenceResource] =
      concepts.toSeq.flatMap(concept => findConceptReferenceResource(concept).map(res => (concept -> res))).toMap

    assertResult(true) {
      val expectedDocUris =
        Set(
          URI.create("http://www.nltaxonomie.nl/nt11/venj/20161214/dictionary/venj-bw2-data-ref.xml"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-venj-bw2-data-ref.xml"))

      val foundDocUris =
        conceptReferences.filterKeys(_.namespaceUriOption.contains(VenjBw2iNamespace)).values.map(_.docUri).toSet

      foundDocUris.subsetOf(expectedDocUris)
    }
  }

  //
  // Exercise 3
  //

  it should "support retrieval of parent-child relationships" in {
    // Find all abstract root sources of parent-child relationships in a given ELR.

    // Implement the following function. Somewhat challenging.

    // Hint: first find the parent-child relationships of the given ELR, then determine their root source
    // concepts (that is, sources that are not targets in any of the found relationships), and finally filter
    // those concepts that are abstract (which follows from their global element declarations).

    def findAllAbstractRootSourceConceptsInParentChildNetwork(parentChildElr: String): Set[EName] = {
      // Use the taxo variable to query for parent-child relationships and concept declarations

      ???
    }

    val elr = "urn:kvk:linkrole:notes-income-tax-expense"

    val expectedAbstractTopLevelSources =
      Set(EName(KvkAbstrNamespace, "IncomeTaxExpenseDisclosureTitle"))

    assertResult(expectedAbstractTopLevelSources) {
      findAllAbstractRootSourceConceptsInParentChildNetwork(elr)
    }
  }

  //
  // Exercise 4
  //

  it should "support retrieval of parent-child relationship paths" in {
    // Find all longest parent-child relationship paths in a given ELR, outgoing from a given concept.

    // Implement the following function. The challenge is in finding the most appropriate TQA query method
    // for this task.

    def findLongestParentChildRelationshipPaths(
      startConcept: EName,
      elr:          String): immutable.IndexedSeq[ParentChildRelationshipPath] = {

      // Return all longest parent-child relationship paths starting with the given concept,
      // where each relationship in the path has the given ELR.

      ???
    }

    val elr = "urn:kvk:linkrole:balance-sheet"
    val startConcept = EName(KvkAbstrNamespace, "BalanceSheetCompleteTitle")

    val relationshipPaths = findLongestParentChildRelationshipPaths(startConcept, elr)

    assertResult(Set(startConcept)) {
      relationshipPaths.map(_.sourceConcept).toSet
    }

    val someExpectedLeafConcepts =
      Set(
        EName(VenjBw2iNamespace, "BalanceSheetBeforeAfterAppropriationResults"),
        EName(VenjBw2iNamespace, "IntangibleAssets"),
        EName(VenjBw2iNamespace, "PropertyPlantEquipment"),
        EName(RjiNamespace, "InvestmentProperties"),
        EName(VenjBw2iNamespace, "FinancialAssets"),
        EName(VenjBw2iNamespace, "Inventories"),
        EName(RjiNamespace, "ConstructionContractsAssets"),
        EName(VenjBw2iNamespace, "Receivables"),
        EName(VenjBw2iNamespace, "SecuritiesCurrent"),
        EName(VenjBw2iNamespace, "Provisions"),
        EName(VenjBw2iNamespace, "LiabilitiesNoncurrent"),
        EName(VenjBw2iNamespace, "ShareCapital"),
        EName(VenjBw2iNamespace, "SharePremium"),
        EName(VenjBw2iNamespace, "RevaluationReserve"),
        EName(VenjBw2iNamespace, "StatutoryReserves"),
        EName(VenjBw2iNamespace, "ReservesOther"),
        EName(VenjBw2iNamespace, "RetainedEarnings"),
        EName(VenjBw2iNamespace, "ResultForTheYear"))

    assertResult(true) {
      someExpectedLeafConcepts.subsetOf(relationshipPaths.map(_.targetConcept).toSet)
    }

    // All found relationships have the same ELR

    assertResult(Set(elr)) {
      relationshipPaths.flatMap(_.relationships).map(_.elr).toSet
    }
  }

  //
  // Exercise 5
  //

  it should "support retrieval of parent-child relationships and affected concept declarations" in {
    // Most parent-child tree root concepts are abstract. Here we are going to find all parent-child network root concepts that are concrete item concepts,
    // hoping to find none. The root concepts of a parent-child network are the top-level concepts, that is the source concepts that are not
    // target concepts in the same network (ELR).

    // Implement the following functions. Somewhat challenging. The challenge is mainly in using the appropriate TQA query API (and DOM level) methods.

    def findAllParentChildTreeRootConcepts(elr: String): Set[EName] = {
      // Find all source concepts that are not target concepts in the parent-child trees with the given ELR.

      ???
    }

    def findAllParentChildTreeRootConceptsForAllElrs: Set[EName] = {
      // Use function findAllParentChildTreeRootConcepts per ELR, and combine the results

      ???
    }

    def findAllParentChildTreeRootConceptsThatAreConcreteItemConcepts: Set[EName] = {
      // Use function findAllParentChildTreeRootConceptsForAllElrs

      ???
    }

    assertResult(Set()) {
      findAllParentChildTreeRootConceptsThatAreConcreteItemConcepts
    }

    assertResult(true) {
      findAllParentChildTreeRootConceptsForAllElrs.contains(EName(KvkAbstrNamespace, "BalanceSheetCompleteTitle"))
    }
  }

  //
  // Exercise 6
  //

  it should "support retrieval of definition relationships" in {
    // Finds all different arcroles of definition relationships in the DTS.

    // Implement the following variable initialization. The challenge is using the appropriate query API method.
    // After all, there is no specific method for querying definition relationships in general. Hint, definition
    // relationships, like presentation and calculation relationships, are inter-concept relationships.

    val arcrolesOfDefinitionRelationships: Set[String] = {
      // Find all definition relationships and return their arcroles in a Set

      ???
    }

    assertResult(Set(
      "http://xbrl.org/int/dim/arcrole/domain-member",
      "http://xbrl.org/int/dim/arcrole/hypercube-dimension",
      "http://xbrl.org/int/dim/arcrole/all",
      "http://xbrl.org/int/dim/arcrole/dimension-domain")) {

      arcrolesOfDefinitionRelationships
    }
  }

  //
  // Exercise 7
  //

  it should "support retrieval of generic element-label relationships" in {
    // Finds all resource roles of element-label relationships of roleType definitions.

    // Implement the following variable initialization. The challenge is using the appropriate query API methods,
    // given that there are no specific methods for retrieving roleType definitions and element-label relationships.
    // Hint: find the roleTypes via the root elements in the taxonomy, and treat element-label relationships as
    // specific kinds of non-standard relationships.

    val resourceRolesOfRoleTypeLabelRelationships: Set[String] = {
      // Find all roleType definitions, and their outgoing element-label relationships. Return their resource roles.
      // It may be needed to cast the resolvedTo of an element-label relationship to type XLinkResource.

      ???
    }

    assertResult(Set("http://www.xbrl.org/2008/role/label")) {
      resourceRolesOfRoleTypeLabelRelationships
    }
  }

  //
  // Exercise 8
  //

  it should "support retrieval of custom generic relationships" in {
    // Find all custom SBR linkrole order (generic) relationships.

    val customArcrole = "http://www.nltaxonomie.nl/2011/arcrole/linkrole-order"

    // Implement the following variable initialization. Easy after the preceding exercises,
    // although TQA knows nothing about custom SBR generic relationships.

    val sbrLinkroleOrderRelationships: immutable.IndexedSeq[OtherNonStandardRelationship] = {
      // Recognize these linkrole order relationships by the custom arcrole above.

      ???
    }

    assertResult(Set(customArcrole)) {
      sbrLinkroleOrderRelationships.map(_.arcrole).toSet
    }

    assertResult(true) {
      sbrLinkroleOrderRelationships.forall(_.resolvedFrom.resolvedElem.isInstanceOf[RoleType])
    }
    assertResult(true) {
      sbrLinkroleOrderRelationships.forall(_.resolvedTo.resolvedElem.isInstanceOf[XLinkResource])
    }

    assertResult(Set(EName("{http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension}linkroleOrder"))) {
      sbrLinkroleOrderRelationships.map(_.resolvedTo.xlinkLocatorOrResource.backingElem.resolvedName).toSet
    }
  }

  //
  // Exercise 9
  //

  it should "support queries for properties of concepts (like substitution groups) in parent-child trees" in {
    // Find all substitution groups of parent-child network root concepts and leaf concepts.

    def findAllParentChildRoots(elr: String): Set[EName] = {
      val parentChildRels = taxo.filterParentChildRelationships(_.elr == elr)

      val sources = parentChildRels.map(_.sourceConceptEName).toSet
      val targets = parentChildRels.map(_.targetConceptEName).toSet
      sources.diff(targets)
    }

    def findAllParentChildLeaves(elr: String): Set[EName] = {
      val parentChildRels = taxo.filterParentChildRelationships(_.elr == elr)

      val sources = parentChildRels.map(_.sourceConceptEName).toSet
      val targets = parentChildRels.map(_.targetConceptEName).toSet
      targets.diff(sources)
    }

    // Implement the following variable initializations, using the functions above. This exercise is not too hard
    // after the preceding exercises.

    val allParentChildRoots: Set[EName] = {
      // Find the parent-child roots per ELR, and combine them.

      ???
    }

    val allParentChildLeaves: Set[EName] = {
      // Find the parent-child leaves per ELR, and combine them.

      ???
    }

    // It would indeed be quite unlikely that a root for one ELR is a leaf for another one.

    assertResult(Set()) {
      allParentChildRoots.intersect(allParentChildLeaves)
    }

    // The roots are all SBR presentation items

    assertResult(Set(EName("{http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension}presentationItem"))) {
      val conceptDecls = allParentChildRoots.flatMap(c => taxo.findConceptDeclaration(c))
      val substGroups = conceptDecls.flatMap(_.globalElementDeclaration.substitutionGroupOption).toSet
      substGroups
    }

    // The leaves are all "plain items"

    assertResult(Set(EName(Namespaces.XbrliNamespace, "item"))) {
      val conceptDecls = allParentChildLeaves.flatMap(c => taxo.findConceptDeclaration(c))
      val substGroups = conceptDecls.flatMap(_.globalElementDeclaration.substitutionGroupOption).toSet
      substGroups
    }
  }

  //
  // Exercise 10
  //

  it should "support queries for kinds of relationships of concrete item concepts" in {
    // Finds all relationship arcroles of standard relationships outgoing from concrete item concepts.

    // Implement the following variable initialization. Not too hard after the preceding exercises.
    // The name of the variable makes clear what value is expected.

    val arcrolesOfStandardRelationshipsOutgoingFromConcreteItemConcepts: Set[String] = {
      ???
    }

    assertResult(Set(
      "http://www.xbrl.org/2003/arcrole/concept-label",
      "http://www.xbrl.org/2003/arcrole/concept-reference",
      "http://www.xbrl.org/2003/arcrole/parent-child")) {

      arcrolesOfStandardRelationshipsOutgoingFromConcreteItemConcepts
    }
  }

  //
  // Exercise 11
  //

  it should "support rewriting specific relationship queries in terms of more general query API methods" in {
    // Specific TQA query API method calls can almost always be replaced by equivalent calls to more general query API methods.
    // For example, querying for parent-child relationships is querying for specific kinds of inter-concept relationships.
    // This is important for 2 reasons: we can always fall back to specific calls of general query API methods, if needed,
    // and we can understand specific query API methods in terms of more general ones.

    // In this exercise, we explore this idea for parent-child relationships.

    val elr = "urn:kvk:linkrole:notes-income-tax-expense"
    val startConcept = EName(KvkAbstrNamespace, "IncomeTaxExpenseDisclosureTitle")

    val parentChildRels = taxo.filterOutgoingParentChildRelationships(startConcept)(_.elr == elr)

    // Implement the following variable initialization. It must return the same as parentChildRels, but using a more
    // general method than filterOutgoingParentChildRelationships.

    val parentChildRels2 = {
      ???
    }

    assertResult(parentChildRels) {
      parentChildRels2
    }
  }

  //
  // Exercise 12
  //

  it should "support interesting queries about label resource uniqueness" in {
  }

  //
  // Exercise 13
  //

  it should "support interesting queries about ELRs having generic labels" in {
  }

  //
  // Exercise 14
  //

  it should "support interesting queries about all locators being used in relationships" in {
  }

  //
  // Exercise 15
  //

  it should "support interesting queries about networks of relationships" in {
  }
}
