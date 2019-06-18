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

import scala.collection.immutable
import scala.reflect.classTag

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.ENames.XLinkArcroleEName
import eu.cdevreeze.tqa.ENames.XLinkFromEName
import eu.cdevreeze.tqa.ENames.XLinkHrefEName
import eu.cdevreeze.tqa.ENames.XLinkRoleEName
import eu.cdevreeze.tqa.ENames.XLinkToEName
import eu.cdevreeze.tqa.ENames.XLinkTypeEName
import eu.cdevreeze.tqa.Namespaces.LinkNamespace
import eu.cdevreeze.tqa.docbuilder.saxon.SaxonDocumentBuilder
import eu.cdevreeze.tqa.base.dom.ExtendedLink
import eu.cdevreeze.tqa.base.dom.LabeledXLink
import eu.cdevreeze.tqa.base.dom.LinkbaseRef
import eu.cdevreeze.tqa.base.dom.SimpleLink
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.XLinkArc
import eu.cdevreeze.tqa.base.dom.XLinkLocator
import eu.cdevreeze.tqa.base.dom.XLinkResource
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.yaidom.core.Path
import net.sf.saxon.s9api.Processor

/**
 * Test specification for low level XLink processing. In this test case, the low level type-safe taxonomy DOM
 * in TQA is shown, but almost exclusively at the level of XLink content.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 * Specifically for this exercise, have a look at the "type-safe DOM" package of TQA, concentrating on XLink,
 * so on types like `SimpleLink`, `ExtendedLink`, `XLinkArc`, `XLinkLocator` and `XLinkResource`.
 *
 * Study the input taxonomy files as well, because the test methods use this input.
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class XLinkSpec extends FlatSpec {

  private val taxoRootDir = new File(classOf[XLinkSpec].getResource("/taxonomy").toURI)

  // Parsing the taxonomy files into a TQA model with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)
  private val docBuilder =
    new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromLocalMirrorRootDirectory(taxoRootDir))

  private val schemaUri =
    URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd")
  private val linkbaseUri =
    URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-verbose-lab-en.xml")

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

  // Below, many exercises are very easy to complete. Only a few exercises are more challenging.

  //
  // Exercise 1
  //

  "Each linkbase reference" should "be an XLink simple link" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    // Let's query for link:linkbaseRef elements.
    // In yaidom the query could be as follows:
    //
    // schema.filterElems(_.resolvedName == LinkLinkbaseRefEName)
    //
    // In TQA, using its type-safe DOM, the query is as follows:

    val linkbaseRefs = schema.findAllElemsOfType(classTag[LinkbaseRef])

    // In a similar manner, retrieve all XLink simple links in the schema (as descendant elements of the root)

    val simpleLinks: immutable.IndexedSeq[SimpleLink] = ???

    // We compare elements defensively by comparing their Paths (relative to the root element).
    // Note that linkbaseRefs must all be XLink simple links.

    assertResult(true) {
      linkbaseRefs.map(_.backingElem.path).toSet.subsetOf(simpleLinks.map(_.backingElem.path).toSet)
    }
  }

  //
  // Exercise 2
  //

  "A simple link" should "have xlink:type 'simple'" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    val simpleLinks = schema.findAllElemsOrSelfOfType(classTag[SimpleLink])

    // Retrieve the XLink types using TQA, and not using yaidom directly. Very easy.

    val xlinkTypesOfSimpleLinks: Set[String] = ???

    assertResult(Set("simple")) {
      xlinkTypesOfSimpleLinks
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(simpleLinks.flatMap(_.attributeOption(XLinkTypeEName)).toSet) {
      xlinkTypesOfSimpleLinks
    }
  }

  //
  // Exercise 3
  //

  it should "have an xlink:href attribute" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    val simpleLinks = schema.findAllElemsOrSelfOfType(classTag[SimpleLink])

    // Retrieve the XLink hrefs using TQA, and not using yaidom directly. Very easy.

    val rawHrefs: Set[URI] = ???

    assertResult(true) {
      val someRawHrefs = Set(
        URI.create("rj-data-documentation-lab-nl.xml"),
        URI.create("rj-data-periodend-lab-en.xml"),
        URI.create("rj-data-terse-lab-en.xml"),
        URI.create("../../../venj/20161214/dictionary/venj-bw2-data-ref.xml"),
        URI.create("rj-venj-bw2-data-ref.xml"))

      someRawHrefs.subsetOf(rawHrefs)
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(simpleLinks.flatMap(_.attributeOption(XLinkHrefEName).map(u => URI.create(u))).toSet) {
      rawHrefs
    }
  }

  //
  // Exercise 4
  //

  it should "have an absolute href after resolution against the 'base' URI" in {
    val schema = taxonomyBase.rootElemUriMap(schemaUri)

    val simpleLinks = schema.findAllElemsOfType(classTag[SimpleLink])

    // Retrieve the XLink hrefs (but made absolute) using TQA, and not using yaidom directly.
    // Remember that the schema holds the original HTTP document URI! This is an easy exercise.

    val absoluteHrefs: Set[URI] = ???

    assertResult(true) {
      val someAbsoluteHrefs = Set(
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-documentation-lab-nl.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-periodend-lab-en.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-terse-lab-en.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/venj/20161214/dictionary/venj-bw2-data-ref.xml"),
        URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-venj-bw2-data-ref.xml"))

      someAbsoluteHrefs.subsetOf(absoluteHrefs)
    }
  }

  //
  // Exercise 5
  //

  "An extended link" should "have xlink:type 'extended'" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val extendedLinks = linkbase.findAllElemsOrSelfOfType(classTag[ExtendedLink])

    // Retrieve the XLink types using TQA, and not using yaidom directly. Very easy.

    val xlinkTypesOfExtendedLinks: Set[String] = ???

    assertResult(Set("extended")) {
      xlinkTypesOfExtendedLinks
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(extendedLinks.flatMap(_.attributeOption(XLinkTypeEName)).toSet) {
      xlinkTypesOfExtendedLinks
    }
  }

  //
  // Exercise 6
  //

  it should "have an extended link role (ELR)" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val extendedLinks = linkbase.findAllElemsOrSelfOfType(classTag[ExtendedLink])

    // Retrieve the extended link roles (ELRs) using TQA, and not using yaidom directly. Very easy.

    val elrsOfExtendedLinks: Set[String] = ???

    assertResult(Set("http://www.xbrl.org/2003/role/link")) {
      elrsOfExtendedLinks
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(extendedLinks.flatMap(_.attributeOption(XLinkRoleEName)).toSet) {
      elrsOfExtendedLinks
    }
  }

  //
  // Exercise 7
  //

  it should "contain XLink arcs all having both an xlink:from and xlink:to" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val extendedLinks = linkbase.findAllElemsOrSelfOfType(classTag[ExtendedLink])
    val firstExtendedLink = extendedLinks.head

    // Retrieve the xlink:from and xlink:to values in the arcs of the first extended link.
    // Use TQA, and do not use yaidom directly. Find all arcs in the link, and return the from/to pairs.
    // This exercise is relatively easy.

    val fromToPairsInFirstExtendedLink: Set[(String, String)] = ???

    assertResult(true) {
      val someFromToPairs: Set[(String, String)] =
        Set(
          ("rj-i_EBITA_loc", "rj-i_EBITA_verboseLabel_en"),
          ("rj-i_IncreaseDecreaseCredits_loc", "rj-i_IncreaseDecreaseCredits_verboseLabel_en"),
          ("rj-i_MemberPayments_loc", "rj-i_MemberPayments_verboseLabel_en"),
          ("rj-i_ReinsurancePremiumsPaid_loc", "rj-i_ReinsurancePremiumsPaid_verboseLabel_en"),
          ("rj-i_TransfersOfRightsReceived_loc", "rj-i_TransfersOfRightsReceived_verboseLabel_en"))

      someFromToPairs.subsetOf(fromToPairsInFirstExtendedLink)
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(
      firstExtendedLink.filterChildElems(_.attributeOption(XLinkTypeEName).contains("arc")).
        map(e => (e.attribute(XLinkFromEName), e.attribute(XLinkToEName))).toSet) {

        fromToPairsInFirstExtendedLink
      }
  }

  //
  // Exercise 8
  //

  it should "contain XLink locators and resources as arc xlink:from or xlink:to" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val extendedLinks = linkbase.findAllElemsOrSelfOfType(classTag[ExtendedLink])
    val firstExtendedLink = extendedLinks.head

    // The XLink locators and resources in the first extended link are stored in a Map.
    // The Map key is the xlink:label. This Map can be used below.

    // Note that multiple XLink locators and resources may share the same xlink:label.
    // This is not a common situation, but nevertheless allowed. In this exercise, it
    // can be assumed that each xlink:label corresponds to precisely one XLink locator or resource.
    // It can also be assumed here that the 'from' can be cast to an XLinkLocator and
    // the 'to' to an XLinkResource.

    val labeledXLinkByXLinkLabel: Map[String, immutable.IndexedSeq[LabeledXLink]] =
      firstExtendedLink.labeledXlinkMap

    // Retrieve the xlink:from and xlink:to values in the arcs of the first extended link.
    // In our example, the xlink:from is an XLink locator, and the xlink:to is an XLink resource.
    // Return the result as a set of pairs of locator hrefs (made absolute) and resource Paths.
    // Use TQA, and do not use yaidom directly. This is a challenging exercise.

    val fromToPairsInFirstExtendedLink: Set[(URI, Path)] = ???

    assertResult(true) {
      val someFromToPairs: Set[(URI, Path)] =
        Set(
          (
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_EBITA"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[12]")),
          (
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_IncreaseDecreaseCredits"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[20]")),
          (
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_MemberPayments"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[18]")),
          (
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_ReinsurancePremiumsPaid"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[43]")),
          (
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_TransfersOfRightsReceived"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[52]")))

      someFromToPairs.subsetOf(fromToPairsInFirstExtendedLink)
    }
  }

  //
  // Exercise 9
  //

  "An XLink arc" should "have xlink:type 'arc'" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val arcs = linkbase.findAllElemsOrSelfOfType(classTag[XLinkArc])

    // Retrieve the XLink types using TQA, and not using yaidom directly. Very easy.

    val xlinkTypesOfArcs: Set[String] = ???

    assertResult(Set("arc")) {
      xlinkTypesOfArcs
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(arcs.flatMap(_.attributeOption(XLinkTypeEName)).toSet) {
      xlinkTypesOfArcs
    }
  }

  //
  // Exercise 10
  //

  it should "have an xlink:arcrole" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val arcs = linkbase.findAllElemsOrSelfOfType(classTag[XLinkArc])

    // Retrieve the arcroles using TQA, and not using yaidom directly. Very easy.

    val arcrolesOfArcs: Set[String] = ???

    assertResult(Set("http://www.xbrl.org/2003/arcrole/concept-label")) {
      arcrolesOfArcs
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(arcs.flatMap(_.attributeOption(XLinkArcroleEName)).toSet) {
      arcrolesOfArcs
    }
  }

  //
  // Exercise 11
  //

  "An XLink locator" should "have xlink:type 'locator'" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val locators = linkbase.findAllElemsOrSelfOfType(classTag[XLinkLocator])

    // Retrieve the XLink types using TQA, and not using yaidom directly. Very easy.

    val xlinkTypesOfLocators: Set[String] = ???

    assertResult(Set("locator")) {
      xlinkTypesOfLocators
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(locators.flatMap(_.attributeOption(XLinkTypeEName)).toSet) {
      xlinkTypesOfLocators
    }
  }

  //
  // Exercise 12
  //

  it should "have an xlink:href" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val locators = linkbase.findAllElemsOrSelfOfType(classTag[XLinkLocator])

    // Retrieve the XLink locator href attributes.
    // Use TQA, and do not use yaidom directly. Very easy.

    val locatorHrefs: Set[URI] = ???

    assertResult(true) {
      val someLocatorHrefs: Set[URI] =
        Set(
          URI.create("rj-data.xsd#rj-i_EBITA"),
          URI.create("rj-data.xsd#rj-i_IncreaseDecreaseCredits"),
          URI.create("rj-data.xsd#rj-i_MemberPayments"),
          URI.create("rj-data.xsd#rj-i_ReinsurancePremiumsPaid"),
          URI.create("rj-data.xsd#rj-i_TransfersOfRightsReceived"))

      someLocatorHrefs.subsetOf(locatorHrefs)
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(
      linkbase.filterElems(_.attributeOption(XLinkTypeEName).contains("locator")).
        flatMap(e => e.attributeOption(XLinkHrefEName)).map(u => URI.create(u)).toSet) {

        locatorHrefs
      }
  }

  //
  // Exercise 13
  //

  it should "have a xlink:href resolvable as absolute URI" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val locators = linkbase.findAllElemsOrSelfOfType(classTag[XLinkLocator])

    // Retrieve the XLink locator href attributes, resolving them against the document URI.
    // Use TQA, and do not use yaidom directly. Very easy.

    val absoluteLocatorHrefs: Set[URI] = ???

    assertResult(true) {
      val someLocatorHrefs: Set[URI] =
        Set(
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_EBITA"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_IncreaseDecreaseCredits"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_MemberPayments"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_ReinsurancePremiumsPaid"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_TransfersOfRightsReceived"))

      someLocatorHrefs.subsetOf(absoluteLocatorHrefs)
    }
  }

  //
  // Exercise 14
  //

  "An XLink resource" should "have xlink:type 'resource'" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val resources = linkbase.findAllElemsOrSelfOfType(classTag[XLinkResource])

    // Retrieve the XLink types using TQA, and not using yaidom directly. Very easy.

    val xlinkTypesOfResources: Set[String] = ???

    assertResult(Set("resource")) {
      xlinkTypesOfResources
    }

    // At a lower level of abstraction, using yaidom directly, we could obtain the same result:

    assertResult(resources.flatMap(_.attributeOption(XLinkTypeEName)).toSet) {
      xlinkTypesOfResources
    }
  }

  //
  // Exercise 15
  //

  "An extended link" should "have 'logical arcs'" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val extendedLinks = linkbase.findAllElemsOrSelfOfType(classTag[ExtendedLink])
    val firstExtendedLink = extendedLinks.head

    // The XLink locators and resources in the first extended link are stored in a Map.
    // The Map key is the xlink:label. This Map can be used below.

    // Note that multiple XLink locators and resources may share the same xlink:label.
    // This is not a common situation, but nevertheless allowed. In this exercise, it
    // can be assumed that each xlink:label corresponds to precisely one XLink locator or resource.
    // It can also be assumed here that the 'from' can be cast to an XLinkLocator and
    // the 'to' to an XLinkResource.

    val labeledXLinkByXLinkLabel: Map[String, immutable.IndexedSeq[LabeledXLink]] =
      firstExtendedLink.labeledXlinkMap

    // Retrieve the "logical arcs" in the first extended link.
    // Each such "logical arc" contains the parent link ELR, the arc's arcrole, the xlink:from
    // locator as its absolute href and the xlink:to resource as its Path.
    // Use TQA, and do not use yaidom directly. This is a challenging exercise, but very similar
    // to exercise 8.

    val logicalArcsInFirstExtendedLink: Set[XLinkSpec.ArcFromLocatorToResource] = ???

    assertResult(true) {
      val parentLinkElr = "http://www.xbrl.org/2003/role/link"
      val arcrole = "http://www.xbrl.org/2003/arcrole/concept-label"

      val someLogicalArcs: Set[XLinkSpec.ArcFromLocatorToResource] =
        Set(
          XLinkSpec.ArcFromLocatorToResource(
            parentLinkElr,
            arcrole,
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_EBITA"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[12]")),
          XLinkSpec.ArcFromLocatorToResource(
            parentLinkElr,
            arcrole,
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_IncreaseDecreaseCredits"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[20]")),
          XLinkSpec.ArcFromLocatorToResource(
            parentLinkElr,
            arcrole,
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_MemberPayments"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[18]")),
          XLinkSpec.ArcFromLocatorToResource(
            parentLinkElr,
            arcrole,
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_ReinsurancePremiumsPaid"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[43]")),
          XLinkSpec.ArcFromLocatorToResource(
            parentLinkElr,
            arcrole,
            URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_TransfersOfRightsReceived"),
            Path.fromResolvedCanonicalXPath(s"/*/{$LinkNamespace}labelLink[1]/{$LinkNamespace}label[52]")))

      someLogicalArcs.subsetOf(logicalArcsInFirstExtendedLink)
    }
  }
}

object XLinkSpec {

  /**
   * Ad-hoc "struct" holding the data of a "logical" arc from an XLink locator to an XLink resource.
   */
  final case class ArcFromLocatorToResource(val parentLinkElr: String, val arcrole: String, val from: URI, val to: Path)
}
