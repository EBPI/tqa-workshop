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

import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax

/**
 * This program groups all XML elements in an XML input file by expanded name, and shows the expanded
 * name and element count per group.
 *
 * What the XML Namespaces specification calls "expanded name" is often referred to as "qualified name".
 * Yet this is not the same as the lexical qualified name that can be used in XML documents.
 * Hence when someone talks about "qualified names", always consider if expanded names are meant,
 * or lexical qualified names. For example, the Java JDK `QName` class represents expanded names.
 *
 * For a very good introduction to XML Namespaces, see [[http://www.lenzconsulting.com/namespaces/]].
 * This article uses the terms "expanded name" and "qualified name" correctly.
 *
 * The '''yaidom''' XML querying library ([[https://github.com/dvreeze/yaidom]]) uses classes `EName`
 * and `QName` for both terms.
 *
 * This program uses yaidom for counting element ENames.
 *
 * Given the root element of the input XML, method `findAllElemsOrSelf` finds all descendant-or-self
 * elements of the root element. Note how yaidom interoperates nicely with the Scala Collections API.
 *
 * There are very many ways to process XML, and consequently there are very many ways to implement the
 * functionality of this program. For example:
 * <ul>
 * <li>SAX parsing</li>
 * <li>W3C DOM (or DOM-LS) parsing</li>
 * <li>StAX parsing</li>
 * <li>XOM</li>
 * <li>JDOM</li>
 * <li>JAXB</li>
 * <li>Scala-XML</li>
 * </ul>
 *
 * The caller of this program is encouraged to use an alternative to yaidom to implement the
 * same functionality, such that the solution is as precise as this one, and at least as easy to
 * implement (in Scala or Java).
 *
 * @author Chris de Vreeze
 */
object CountElementsByExpandedName {

  def main(args: Array[String]): Unit = {
    // The program may get one argument for the XML input file path, taking a default otherwise

    require(args.size <= 1, s"Usage: CountElementsByExpandedName [ <XML input file path> ]")

    val inputXmlFile =
      if (args.isEmpty) {
        val inputFileUri = classOf[CountElementsByExpandedName].getResource("/sample-Instance-Proof.xml").toURI
        new File(inputFileUri)
      } else {
        new File(args(0))
      }

    // We are going to parse the document as native yaidom "simple" Document.

    val docParser = DocumentParserUsingSax.newInstance()

    val doc = docParser.parse(inputXmlFile)

    // Find all descendant-or-self elements of the root element.
    // This same query API call could have been used if the document was backed by another "XML backend" than native yaidom, such as Saxon

    val allElems = doc.documentElement.findAllElemsOrSelf

    // The resolved name of an element is the expanded name...
    val elemGroups = allElems.groupBy(_.resolvedName)

    println(s"Showing all element counts (per element expanded name):")
    println()

    elemGroups.toIndexedSeq.sortBy(_._1.toString) foreach {
      case (ename, elements) =>
        println(s"Expanded name: $ename. Element count: ${elements.size}")
    }

    println()
    println(s"Most common element expanded names (at most 10 shown):")
    println()

    elemGroups.toIndexedSeq.sortBy(_._2.size).reverse.take(10) foreach {
      case (ename, elements) =>
        println(s"Expanded name: $ename. Element count: ${elements.size}")
    }
  }
}

class CountElementsByExpandedName
