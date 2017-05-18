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

import java.io.File

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.simple.Elem

/**
 * This program shows the (yaidom) Paths (relative to the root) of all elements occurring in an input XML.
 * These Paths are shown by adding attributes "yaidom-path" to all elements in the tree.
 *
 * For readability, copy and paste the program XML output into a file, and open this file in a browser.
 *
 * This program has educational value only. It shows the correspondence between Paths, root elements
 * and the element itself.
 *
 * The caller of this program is encouraged to use an alternative to yaidom to implement the
 * same functionality, such that the solution is as precise as this one, and at least as easy to
 * implement (in Scala or Java).
 *
 * @author Chris de Vreeze
 */
object ShowPaths {

  def main(args: Array[String]): Unit = {
    // The program may get one argument for the XML input file path, taking a default otherwise

    require(args.size <= 1, s"Usage: ShowPaths [ <XML input file path> ]")

    val inputXmlFile =
      if (args.isEmpty) {
        val inputFileUri = classOf[ShowPaths].getResource("/sample-Instance-Proof.xml").toURI
        new File(inputFileUri)
      } else {
        new File(args(0))
      }

    // We are going to parse the document as native yaidom "simple" Document.

    val docParser = DocumentParserUsingSax.newInstance()
    val docPrinter = DocumentPrinterUsingDom.newInstance()

    val doc = docParser.parse(inputXmlFile)

    val indexedRootElem = indexed.Document(doc).documentElement
    val paths = indexedRootElem.findAllElemsOrSelf.map(_.path).toSet

    // Add the yaidom-path attribute.

    def enrich(elm: Elem, path: Path): Elem = {
      elm.plusAttribute(QName("yaidom-path"), path.toResolvedCanonicalXPath)
    }

    // Note we use a powerful functional update method, at the expense of performance

    val enrichedDoc =
      doc.withDocumentElement(
        doc.documentElement.updateElemsOrSelf(paths)({ case (e, path) => enrich(e, path) }).prettify(2))

    val enrichedXmlString = docPrinter.print(enrichedDoc)

    println(enrichedXmlString)
  }
}

class ShowPaths
