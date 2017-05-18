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

import org.scalatest.FlatSpec

import eu.cdevreeze.tqa.Namespaces.XbrliNamespace
import eu.cdevreeze.tqa.backingelem.nodeinfo.SaxonDocumentBuilder
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import net.sf.saxon.s9api.Processor

/**
 * Test specification for yaidom as a "theory", showing how the yaidom query API "hangs together". Note that this
 * "theory" holds regardless of the underlying concrete element implementation, such as native yaidom or Saxon tiny trees.
 * Also note that yaidom interoperates nicely with the Scala Collections API.
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
class QueryApiAsTheorySpec extends FlatSpec {

  // Parsing the instance into an "BackingElemApi" element with Saxon, although the use of Saxon does not influence the querying code.

  private val processor = new Processor(false)
  private val docBuilder = new SaxonDocumentBuilder(processor.newDocumentBuilder(), (uri => uri))

  private val rootElem: BackingElemApi =
    docBuilder.build(classOf[QueryApiAsTheorySpec].getResource("/sample-Instance-Proof.xml").toURI)

  private def isInXbrlNamespace(elem: BackingElemApi): Boolean = {
    elem.resolvedName.namespaceUriOption.contains(XbrliNamespace)
  }

  // Below, the names of the tests should be clear instructions as to how the tests should be made to run

  // "Re-implementing" filterChildElems, filterElems and filterElemsOrSelf
  
  //
  //  Assignment 1
  //

  "The yaidom query API as theory" should "support writing 'filterChildElems' in terms of 'findAllChildElems' and the Scala Collections API" in {
    assertResult(rootElem.filterChildElems(isInXbrlNamespace)) {
      val allChildElems = rootElem.findAllChildElems

      // Use the Scala Collections API to turn allChildElems into the desired result
      allChildElems.filter(isInXbrlNamespace)
    }
  }

  //
  //  Assignment 2
  //

  it should "support writing 'filterElems' in terms of 'findAllElems' and the Scala Collections API" in {
    assertResult(rootElem.filterElems(isInXbrlNamespace)) {
      val allElems = rootElem.findAllElems

      // Use the Scala Collections API to turn allElems into the desired result
      allElems.filter(isInXbrlNamespace)
    }
  }

  //
  //  Assignment 3
  //

  it should "support writing 'filterElemsOrSelf' in terms of 'findAllElemsOrSelf' and the Scala Collections API" in {
    assertResult(rootElem.filterElemsOrSelf(isInXbrlNamespace)) {
      val allElemsOrSelf = rootElem.findAllElemsOrSelf

      // Use the Scala Collections API to turn allElemsOrSelf into the desired result
      allElemsOrSelf.filter(isInXbrlNamespace)
    }
  }

  // "Re-implementing" findAllChildElems, findAllElems and findAllElemsOrSelf

  //
  //  Assignment 4
  //

  it should "support writing 'findAllChildElems' in terms of 'filterChildElems'" in {
    assertResult(rootElem.findAllChildElems) {
      rootElem.filterChildElems(e => true)
    }
  }

  //
  //  Assignment 5
  //

  it should "support writing 'findAllElems' in terms of 'filterElems'" in {
    assertResult(rootElem.findAllElems) {
      rootElem.filterElems(e => true)
    }
  }

  //
  //  Assignment 6
  //

  it should "support writing 'findAllElemsOrSelf' in terms of 'filterElemsOrSelf'" in {
    assertResult(rootElem.findAllElemsOrSelf) {
      rootElem.filterElemsOrSelf(e => true)
    }
  }

  // "Re-implementing" findTopmostElems and findTopmostElemsOrSelf.
  // This is more challenging than the other exercises in this test!

  //
  //  Assignment 7
  //

  it should "support writing 'findTopmostElems' in terms of 'filterElems'" in {
    assertResult(rootElem.findTopmostElems(isInXbrlNamespace)) {
      rootElem.filterElems(e => isInXbrlNamespace(e) && e.findAncestor(isInXbrlNamespace).forall(_ == rootElem))
    }
  }

  //
  //  Assignment 8
  //

  it should "support writing 'findTopmostElemsOrSelf' in terms of 'filterElemsOrSelf'" in {
    assertResult(rootElem.findTopmostElemsOrSelf(isInXbrlNamespace)) {
      rootElem.filterElemsOrSelf(e => isInXbrlNamespace(e) && e.findAncestor(isInXbrlNamespace).isEmpty)
    }
  }

  //
  //  Assignment 9
  //

  it should "support writing 'findTopmostElems' in terms of 'findTopmostElemsOrSelf' and 'findAllChildElems'" in {
    assertResult(rootElem.findTopmostElems(isInXbrlNamespace)) {
      rootElem.findAllChildElems flatMap (e => e.findTopmostElemsOrSelf(isInXbrlNamespace))
    }
  }

  // "Re-implementing" findChildElem, findElem and findElemOrSelf

  //
  //  Assignment 10
  //

  it should "support writing 'findChildElem' in terms of 'filterChildElems'" in {
    assertResult(rootElem.findChildElem(isInXbrlNamespace)) {
      rootElem.filterChildElems(isInXbrlNamespace).headOption
    }
  }

  //
  //  Assignment 11
  //

  it should "support writing 'findElem' in terms of 'filterElems'" in {
    assertResult(rootElem.findElem(isInXbrlNamespace)) {
      rootElem.filterElems(isInXbrlNamespace).headOption
    }
  }

  //
  //  Assignment 12
  //

  it should "support writing 'findElemOrSelf' in terms of 'filterElemsOrSelf'" in {
    assertResult(rootElem.findElemOrSelf(isInXbrlNamespace)) {
      rootElem.filterElemsOrSelf(isInXbrlNamespace).headOption
    }
  }

  // Scopes and Declarations

  //
  //  Assignment 13
  //

  it should "support getting each element scope in terms of the declarations and parent scope" in {
    // Implement function getScope in terms of its namespace declarations and the scope of its parent element.
    // Mind the possibility the there is no parent element (for the root itself, of course).
    // Use functions like "namespaces", "parentOption" and "scope".

    def getScope(e: BackingElemApi): Scope = {
      val parentScope = e.parentOption.map(_.scope).getOrElse(Scope.Empty)
      parentScope.resolve(e.namespaces)
    }

    assertResult(rootElem.findAllElemsOrSelf.map(_.scope)) {
      rootElem.findAllElemsOrSelf.map(getScope)
    }
  }

  // QNames and ENames

  //
  //  Assignment 14
  //

  it should "support getting each element EName in terms of its element QName and its Scope" in {
    // Implement function getEName in terms of its QName and Scope.

    def getEName(e: BackingElemApi): EName =
      e.scope.resolveQNameOption(e.qname).get

    assertResult(rootElem.findAllElemsOrSelf.map(_.resolvedName)) {
      rootElem.findAllElemsOrSelf.map(getEName)
    }
  }

  // A similar property holds for attribute ENames/QNames, but for attributes there is no default namespace.
  // Feel free to add and implement that attribute name test.

  // Paths

  //
  //  Assignment 15
  //

  it should "support getting each element in terms of its (navigation) path and root element" in {
    // Implement function getElement.

    def getElement(rootElem: BackingElemApi, path: Path): BackingElemApi =
      rootElem.getElemOrSelfByPath(path)

    // In order not to lean too much on element equality, we only compare the collections of element ENames

    assertResult(rootElem.findAllElemsOrSelf.map(_.resolvedName)) {
      rootElem.findAllElemsOrSelf.map(e => getElement(e.rootElem, e.path)).map(_.resolvedName)
    }
  }
}
