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

import eu.cdevreeze.tqa.ENames.XmlBaseEName
import eu.cdevreeze.tqa.backingelem.nodeinfo.docbuilder.SaxonDocumentBuilder
import eu.cdevreeze.tqa.base.dom.ExtendedLink
import eu.cdevreeze.tqa.base.dom.TaxonomyBase
import eu.cdevreeze.tqa.base.dom.TaxonomyDocument
import eu.cdevreeze.tqa.base.dom.XLinkLocator
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import net.sf.saxon.s9api.Processor

/**
 * Test specification for XML Base processing, in an XLink context. In this test case, the low level type-safe
 * taxonomy DOM in TQA is shown, but almost exclusively at the level of XLink content.
 *
 * Exercise: fill in the needed implementations (replacing the "???"), and make this test spec run successfully.
 *
 * To do this exercise, make sure to have the API documentation of the TQA and yaidom libraries available.
 * Specifically for this exercise, have a look at the "type-safe DOM" package of TQA, concentrating on XLink,
 * so on types like `SimpleLink`, `ExtendedLink`, `XLinkArc`, `XLinkLocator` and `XLinkResource`.
 *
 * Study the input taxonomy files as well, because the test methods use this input. Note that they
 * occur in the taxonomy-edited tree!
 *
 * Make sure to use a Java 8 JDK.
 *
 * @author Chris de Vreeze
 */
class XmlBaseSpec extends FlatSpec {

  private val taxoRootDir = new File(classOf[XmlBaseSpec].getResource("/taxonomy-edited").toURI)

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

  "The base URI of an XML root element" should "default to the document URI but be affected by an xml:base attribute, if any" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    // All elements in the DOM tree share the same document URI reference

    assertResult(Set(linkbaseUri)) {
      linkbase.findAllElemsOrSelf.map(_.docUri).toSet
    }

    // Note that in this case the document URI and root element base URI are not the same!

    assertResult(true) {
      linkbase.backingElem.baseUri != linkbaseUri
    }

    assertResult(URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data-verbose-lab-en.xml")) {
      linkbase.backingElem.baseUri
    }

    // The relation between the root element base URI and document URI (in this case) is as follows:

    assertResult(linkbase.docUri.resolve(URI.create(linkbase.attribute(XmlBaseEName)))) {
      linkbase.backingElem.baseUri
    }

    // Now, using yaidom's XmlBaseSupport object, obtain the base URI from the parent base URI and the element itself.
    // Try to write this function in such a way that it works for any element and not just the root element.
    // Use function XmlBaseSupport.findBaseUriByParentBaseUri, and be careful in passing it the correct arguments.
    // Once function XmlBaseSupport.findBaseUriByParentBaseUri is understood, this is a relatively easy exercise.

    def getBaseUriFromParentBaseUriAndElem(elem: BackingElemApi): URI = {
      ???
    }

    assertResult(getBaseUriFromParentBaseUriAndElem(linkbase.backingElem)) {
      linkbase.backingElem.baseUri
    }
  }

  //
  // Exercise 2
  //

  "The base URI of any XML element" should "default to the document URI but be affected by ancestor-or-self xml:base attributes, if any" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val extendedLinks = linkbase.findAllElemsOrSelfOfType(classTag[ExtendedLink])
    val firstExtendedLink = extendedLinks.head

    // Note that in this case the document URI and (first) extended link base URI are not the same!

    assertResult(true) {
      firstExtendedLink.backingElem.baseUri != linkbaseUri
    }

    assertResult(URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/")) {
      firstExtendedLink.backingElem.baseUri
    }

    // The relation between the root element base URI and document URI (in this case) is as follows:

    assertResult(linkbase.docUri.resolve(URI.create(linkbase.attribute(XmlBaseEName))).
      resolve(URI.create(firstExtendedLink.attribute(XmlBaseEName)))) {

      firstExtendedLink.backingElem.baseUri
    }

    // Now, using yaidom's XmlBaseSupport object, obtain the base URI from the document URI.
    // Try to write this function in such a way that it works for any element and not just the root element.
    // This time, use function XmlBaseSupport.findBaseUriByDocUriAndPath, and be careful in passing it the correct arguments.
    // Once function XmlBaseSupport.findBaseUriByDocUriAndPath is understood, this is a relatively easy exercise.

    def getBaseUriFromDocUriAndRootElemAndPath(elem: BackingElemApi): URI = {
      ???
    }

    assertResult(getBaseUriFromDocUriAndRootElemAndPath(firstExtendedLink.backingElem)) {
      firstExtendedLink.backingElem.baseUri
    }
  }

  //
  // Exercise 3
  //

  "An extended link" should "respect XML Base attributes when resolving locator href URIs" in {
    val linkbase = taxonomyBase.rootElemUriMap(linkbaseUri)

    val locators = linkbase.findAllElemsOfType(classTag[XLinkLocator])

    // Get the locator href made absolute, using its base URI. Very easy.

    def getAbsoluteHref(locator: XLinkLocator): URI = ???

    assertResult(true) {
      val someLocatorHrefs: Set[URI] =
        Set(
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_EBITA"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_IncreaseDecreaseCredits"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_MemberPayments"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_ReinsurancePremiumsPaid"),
          URI.create("http://www.nltaxonomie.nl/nt11/rj/20170419/dictionary/rj-data.xsd#rj-i_TransfersOfRightsReceived"))

      someLocatorHrefs.subsetOf(locators.map(loc => getAbsoluteHref(loc)).toSet)
    }
  }
}
