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

package eu.cdevreeze.tqaworkshop.chapter4

import java.io.File
import java.net.URI

import scala.collection.immutable
import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames
import eu.cdevreeze.tqa.SubstitutionGroupMap
import eu.cdevreeze.tqa.backingelem.nodeinfo.docbuilder.SaxonDocumentBuilder
import eu.cdevreeze.tqa.base.dom.GlobalElementDeclaration
import eu.cdevreeze.tqa.base.dom.ItemDeclaration
import eu.cdevreeze.tqa.base.dom.NamedTypeDefinition
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.TupleDeclaration
import eu.cdevreeze.tqa.base.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.base.taxonomy.BasicTaxonomy
import eu.cdevreeze.tqa.docbuilder.jvm.UriConverters
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.tqaworkshop.xbrlinstance.XbrlInstance
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor

/**
 * Test specification for finding declarations and definitions in (taxonomy) schemas. Schema content is either found
 * within a chosen schema document, using the TQA DOM API, or across collections of schema documents, using the TQA
 * query API (which in turn returns TQA DOM objects or collections thereof).
 *
 * Before doing this exercise, make sure to have some basic understanding of XML Schema (outside the realm of XBRL).
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class SchemaUsageSpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi-backed" XbrlInstance with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)

  private val instanceDocBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromUriConverter(UriConverters.identity))

  private val rootDir = new File(classOf[SchemaUsageSpec].getResource("/taxonomy").toURI)

  private val taxoDocBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromLocalMirrorRootDirectory(rootDir))

  private val xbrlInstance: XbrlInstance = {
    val doc = instanceDocBuilder.build(classOf[SchemaUsageSpec].getResource("/kvk-rpt-jaarverantwoording-2016-nlgaap-klein-publicatiestukken.xbrl").toURI)
    XbrlInstance(doc.documentElement)
  }

  private val SbrDomainMemberItemEName = EName("{http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension}domainMemberItem")
  private val SbrPresentationTupleEName = EName("{http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension}presentationTuple")

  private val taxo: BasicTaxonomy = {
    val docUris = Vector(
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-codes.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-domains.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-tuples.xsd",
      "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data-verbose-lab-en.xml",
      "http://www.nltaxonomie.nl/nt11/sbr/20160610/dictionary/nl-types.xsd").map(s => URI.create(s))

    // Building the taxonomy DOM, one root element per parsed taxonomy document.

    val docs = docUris.map(uri => TaxonomyDocument.build(taxoDocBuilder.build(uri)))

    val taxoBase = TaxonomyBase.build(docs)

    // Building a taxonomy that offers the TQA taxonomy query API.
    // It wraps the "DOM-level taxonomy".

    // Note that it only contains a few taxonomy documents, and is by no means closed under URI resolution,
    // so XLink simple links and locators and xs:import elements may be "dead links".
    // This also means that we may have to explicitly add some substitution group knowledge, which is indeed the case here.
    // More about that later in this exercise.
    // The "relationship factory" argument can now be ignored.

    BasicTaxonomy.build(
      taxoBase,
      SubstitutionGroupMap.from(
        Map(SbrDomainMemberItemEName -> ENames.XbrliItemEName, SbrPresentationTupleEName -> ENames.XbrliTupleEName)),
      DefaultRelationshipFactory.LenientInstance)
  } ensuring (_.relationships.nonEmpty)

  private val RjiNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data"
  private val RjtNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-tuples"
  private val RjdmNamespace = "http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-domains"

  //
  // Exercise 1
  //

  "The TQA DOM" should "support finding a specific global element declaration" in {
    // Finding rj-i:InvestmentProperties facts in instance, and finding the corresponding global element declaration in a taxonomy schema
    // We look up the global element declaration the hard way in this exercise, via the schema root element.

    // Let's first find all rj-i:InvestmentProperties facts in the instance
    // We find 2 facts for item concept rj-i:InvestmentProperties, with fact values 135000 and 130000, respectively

    val RjiInvestmentPropertiesEName = EName(RjiNamespace, "InvestmentProperties")

    assertResult(2) {
      xbrlInstance.filterFacts(_.conceptEName == RjiInvestmentPropertiesEName).size
    }
    assertResult(Set("135000", "130000")) {
      xbrlInstance.filterFacts(_.conceptEName == RjiInvestmentPropertiesEName).map(_.text).toSet
    }

    // If we want the XBRL instance to be valid with respect to the taxonomy, it must at least be schema-valid.
    // This means for our RJ facts that there must be a corresponding global element declaration in the correct
    // RJ schema, and that this schema must be found.

    // The XBRL instance contains a reference to the so-called entrypoint schema, and through a process called DTS
    // discovery the correct RJ schema is found, but that is beyond the scope of this exercise. For now, assume that
    // the correct RJ schema is found and that it is part of the taxonomy.

    // Let's find the root element of the schema for the "rj-i" (target) namespace.

    val xsdRootElem = taxo.findAllXsdSchemas.filter(_.targetNamespaceOption.contains(RjiNamespace)).head

    assertResult(URI.create("http://www.nltaxonomie.nl/nt11/rj/20161214/dictionary/rj-data.xsd")) {
      // The original HTTP URI, not the local file URI, is stored with the schema document root element
      xsdRootElem.docUri
    }

    // Implement getting the global element declaration for the XBRL concept rj-i:InvestmentProperties.
    // Note that XBRL concepts are just global element declarations in terms of XML Schema.

    val conceptDecl: GlobalElementDeclaration = {
      // Use the xsdRootElem above as the starting point for obtaining the correct global element declaration.
      // Also use the GlobalElementDeclaration.targetEName method to match on the correct concept name.

      ???
    }

    assertResult(RjiInvestmentPropertiesEName) {
      // We use the term "target EName" for the combination of target namespace (of the schema root element) and
      // name attribute (of the global element declaration itself).

      conceptDecl.targetEName
    }
    assertResult(RjiInvestmentPropertiesEName) {
      // We know the schema has a target namespace attribute, so this is safe
      val tns = conceptDecl.backingElem.rootElem.attribute(ENames.TargetNamespaceEName)
      val name = conceptDecl.nameAttributeValue
      EName(tns, name)
    }
  }

  //
  // Exercise 2
  //

  "The TQA query API" should "support finding a specific global element declaration" in {
    // Finding the rj-i:InvestmentProperties global element declaration in the taxonomy.
    // We look up the global element declaration the easy way in this exercise, directly from the taxonomy, using its query API,
    // and without first looking up a specific schema document root element.

    val RjiInvestmentPropertiesEName = EName(RjiNamespace, "InvestmentProperties")

    // Again, if we want the XBRL instance to be valid with respect to the taxonomy, it must at least be schema-valid.
    // This means for our RJ facts that there must be a corresponding global element declaration in the correct
    // RJ schema, and that this schema must be found.

    // The XBRL instance contains a reference to the so-called entrypoint schema, and through a process called DTS
    // discovery the correct RJ schema is found, but that is beyond the scope of this exercise. For now, assume that
    // the correct RJ schema is found and that it is part of the taxonomy.

    // Implement getting the global element declaration for the XBRL concept rj-i:InvestmentProperties.
    // Note that XBRL concepts are just global element declarations in terms of XML Schema.

    val conceptDecl: GlobalElementDeclaration = {
      // Use the entire taxonomy called "taxo" as the starting point for obtaining the correct global element declaration.

      ???
    }

    assertResult(RjiInvestmentPropertiesEName) {
      // Again, we use the term "target EName" for the combination of target namespace (of the schema root element) and
      // name attribute (of the global element declaration itself).

      conceptDecl.targetEName
    }
  }

  //
  // Exercise 3
  //

  it should "support finding the type definition of a type (of a global element declaration)" in {
    // Finding the type definition of the type of the rj-i:InvestmentProperties global element declaration in the taxonomy.

    val RjiInvestmentPropertiesEName = EName(RjiNamespace, "InvestmentProperties")

    val conceptDecl: GlobalElementDeclaration =
      taxo.getGlobalElementDeclaration(RjiInvestmentPropertiesEName)

    val typeOption: Option[EName] = conceptDecl.typeOption

    assertResult(conceptDecl.attributeAsResolvedQNameOption(ENames.TypeEName)) {
      typeOption
    }
    assertResult(Some(EName("{http://www.nltaxonomie.nl/nt11/sbr/20160610/dictionary/nl-types}monetaryNoDecimalsItemType"))) {
      typeOption
    }

    // Implement getting the type definition for the above-mentioned type nl-types:monetaryNoDecimalsItemType.

    val typeDef: NamedTypeDefinition = {
      // Use the entire taxonomy called "taxo" for obtaining the correct type definition for the type as EName found above.

      ???
    }

    assertResult(typeOption.get) {
      // We use the term "target EName" for the combination of target namespace (of the schema root element) and
      // name attribute (of the named type definition itself).

      typeDef.targetEName
    }
    assertResult(typeOption.get) {
      // We know the schema has a target namespace attribute, so this is safe
      val tns = typeDef.backingElem.rootElem.attribute(ENames.TargetNamespaceEName)
      val name = typeDef.nameAttributeValue
      EName(tns, name)
    }

    assertResult(Some(EName("{http://www.nltaxonomie.nl/nt11/sbr/20160610/dictionary/nl-types}monetary20ItemType"))) {
      typeDef.baseTypeOption
    }
  }

  //
  // Exercise 4
  //

  it should "support finding out if a substitution group is (directly) for an XBRL concept (item or tuple concept)" in {
    // Finding the optional substitution group of the rj-i:InvestmentProperties global element declaration in the taxonomy.

    val RjiInvestmentPropertiesEName = EName(RjiNamespace, "InvestmentProperties")

    val substitutionGroupOption: Option[EName] = {
      // Use the taxonomy called "taxo" for obtaining the correct global element declaration, and its optional substitution group.

      ???
    }

    assertResult(Some(ENames.XbrliItemEName)) {
      substitutionGroupOption
    }

    // So, in this case it is trivial to find out that the InvestmentProperties concept is an item concept, since the substitution group is xbrli:item.
    // No taxonomy context with substitution group knowledge is needed in this case.

    // If we use the taxonomy's knowledge about substitution groups, we get the same result that we have an item concept here, of course.

    assertResult(true) {
      taxo.getGlobalElementDeclaration(RjiInvestmentPropertiesEName).hasSubstitutionGroup(ENames.XbrliItemEName, taxo.substitutionGroupMap)
    }

    // An item concept is not a tuple concept

    assertResult(false) {
      taxo.getGlobalElementDeclaration(RjiInvestmentPropertiesEName).hasSubstitutionGroup(ENames.XbrliTupleEName, taxo.substitutionGroupMap)
    }

    // Using the taxonomy's substitution group knowledge, we can "lift" the global element declaration to an ItemDeclaration

    assertResult(taxo.getGlobalElementDeclaration(RjiInvestmentPropertiesEName)) {
      taxo.getItemDeclaration(RjiInvestmentPropertiesEName).globalElementDeclaration
    }

    assertResult(false) {
      taxo.findTupleDeclaration(RjiInvestmentPropertiesEName).isDefined
    }
  }

  //
  // Exercise 5
  //

  it should "support finding out if a substitution group is indirectly for an XBRL concept (item or tuple concept)" in {
    // Finding the optional substitution group of the rj-dm:BuildingsMember global element declaration in the taxonomy.

    val RjdmBuildingsMemberEName = EName(RjdmNamespace, "BuildingsMember")

    val substitutionGroupOption: Option[EName] = {
      // Use the taxonomy called "taxo" for obtaining the correct global element declaration, and its optional substitution group.

      ???
    }

    assertResult(Some(SbrDomainMemberItemEName)) {
      substitutionGroupOption
    }

    // Is sbr:domainMemberItem derived from the xbrli:item substitution group? The name suggests so, but that is not enough to know for sure.
    // So we need to know if somewhere in the taxonomy there is a global element declaration for sbr:domainMemberItem which in turn has substitution group
    // xbrli:item (directly or indirectly). Had our taxonomy been a "closed DTS", then we would have found it in document
    // http://www.nltaxonomie.nl/2011/xbrl/xbrl-syntax-extension.xsd, and the taxonomy would therefore have known it is an "item substitution group".

    // Yet in our case we do not have this important schema document in the "taxonomy". Note, however, that the BasicTaxonomy object has been created
    // in such a way that we have provided this substitution group knowledge as extra information. Alas, substitution groups defy local reasoning,
    // since their derivation may span multiple documents that are very far apart.

    // Fortunately we made the taxonomy aware of the sbr:domainMemberItem substitution group being an item substitution group.

    assertResult(true) {
      taxo.getGlobalElementDeclaration(RjdmBuildingsMemberEName).hasSubstitutionGroup(ENames.XbrliItemEName, taxo.substitutionGroupMap)
    }

    // Using the taxonomy's substitution group knowledge, we can "lift" the global element declaration to an ItemDeclaration

    assertResult(taxo.getGlobalElementDeclaration(RjdmBuildingsMemberEName)) {
      taxo.getItemDeclaration(RjdmBuildingsMemberEName).globalElementDeclaration
    }
  }

  //
  // Exercise 6
  //

  it should "support querying for item declarations" in {
    // Finding rj-i item facts in the instance (not idiomatically), and checking that their item concepts in the taxonomy indeed exist

    val itemsInInstance = xbrlInstance.findAllItems.filter(_.resolvedName.namespaceUriOption.contains(RjiNamespace))

    assertResult(true)(itemsInInstance.nonEmpty)

    // Finding the item concepts for these item facts in the taxonomy

    val itemConceptDecls = itemsInInstance.map(_.resolvedName) flatMap { conceptName =>
      // Fast search for optional item declaration given a concept name, using method findItemDeclaration.
      // Returns a non-empty result only for item declarations, not for tuple declarations or non-concepts.
      // Method getItemDeclaration is similar, but does not return an Option, and throws an exception for non-items.

      taxo.findItemDeclaration(conceptName)
    }

    // Check that the item facts are indeed item facts according to the taxonomy

    assertResult(Set()) {
      itemsInInstance.map(_.resolvedName).toSet.diff(itemConceptDecls.map(_.targetEName).toSet)
    }

    // The underlying global element declaration is retained

    assertResult(itemConceptDecls.map(_.targetEName)) {
      itemConceptDecls.map(_.globalElementDeclaration.targetEName)
    }

    // Items are not tuples

    assertResult(true) {
      itemsInInstance.map(_.resolvedName).forall(itemName => taxo.findTupleDeclaration(itemName).isEmpty)
    }

    // Now find these same item declarations using a "filter" method

    assertResult(itemConceptDecls.map(_.targetEName).toSet) {
      // Find the same item concept declarations using a "filter" method on the taxonomy

      val itemDecls: immutable.IndexedSeq[ItemDeclaration] = ???
      itemDecls.map(_.targetEName).toSet
    }

    // The "filter" and "findAll" methods are "bulk" operations, and are too slow for one specific concept EName.
    // Methods like findItemDeclaration and getItemDeclaration are fast, because the taxonomy uses an index with EName keys.
  }

  //
  // Exercise 7
  //

  it should "support querying for tuple declarations" in {
    // Finding rj-t tuple facts in the instance (not idiomatically), and checking that their tuple concepts in the taxonomy indeed exist

    val tuplesInInstance = xbrlInstance.findAllTuples.filter(_.resolvedName.namespaceUriOption.contains(RjtNamespace))

    assertResult(true)(tuplesInInstance.nonEmpty)

    // Finding the tuple concepts for these tuple facts in the taxonomy
    // Note that we told the taxonomy about sbr:presentationTuple, or else no tuple concepts would be found

    val tupleConceptDecls = tuplesInInstance.map(_.resolvedName) flatMap { conceptName =>
      // Fast search for optional tuple declaration given a concept name, using method findTupleDeclaration.
      // Returns a non-empty result only for tuple declarations, not for item declarations or non-concepts.
      // Method getTupleDeclaration is similar, but does not return an Option, and throws an exception for non-tuples.

      taxo.findTupleDeclaration(conceptName)
    }

    // Check that the tuple facts are indeed tuple facts according to the taxonomy

    assertResult(Set()) {
      tuplesInInstance.map(_.resolvedName).toSet.diff(tupleConceptDecls.map(_.targetEName).toSet)
    }

    // The underlying global element declaration is retained

    assertResult(tupleConceptDecls.map(_.targetEName)) {
      tupleConceptDecls.map(_.globalElementDeclaration.targetEName)
    }

    // Tuples are not items

    assertResult(true) {
      tuplesInInstance.map(_.resolvedName).forall(tupleName => taxo.findItemDeclaration(tupleName).isEmpty)
    }

    // Now find these same tuple declarations using a "filter" method

    assertResult(tupleConceptDecls.map(_.targetEName).toSet) {
      // Find the same tuple concept declarations using a "filter" method on the taxonomy

      val tupleDecls: immutable.IndexedSeq[TupleDeclaration] = ???
      tupleDecls.map(_.targetEName).toSet
    }

    // The "filter" and "findAll" methods are "bulk" operations, and are too slow for one specific concept EName.
    // Methods like findTupleDeclaration and getTupleDeclaration are fast, because the taxonomy uses an index with EName keys.
  }
}
