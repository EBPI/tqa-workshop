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
import java.util.logging.Logger

import scala.collection.immutable

import eu.cdevreeze.tqa.docbuilder.saxon.SaxonDocumentBuilder
import eu.cdevreeze.tqa.base.dom.ConceptDeclaration
import eu.cdevreeze.tqa.base.relationship.DefaultRelationshipFactory
import eu.cdevreeze.tqa.base.taxonomybuilder.DefaultDtsCollector
import eu.cdevreeze.tqa.base.taxonomybuilder.DocumentCollector
import eu.cdevreeze.tqa.base.taxonomybuilder.TaxonomyBuilder
import eu.cdevreeze.tqa.docbuilder.jvm.UriResolvers
import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor

/**
 * Program that shows concept substitution groups. TQA is used to retrieve the concept substitution groups.
 *
 * In particular, the '''TQA DOM package''' is used for its model of schema content, and global element declarations
 * in particular. After all, each concept declaration is a global element declaration from the perspective
 * of XML Schema.
 *
 * Yet how do we know if a global element declaration declares an XBRL concept, if the substitution group
 * is not xbrli:item, xbrli:tuple etc.? We have to inspect the entire taxonomy and not just the containing
 * taxonomy schema document itself to reason about substitution group inheritance, and determine in general
 * if a global element declaration declares an XBRL concept (item or tuple concept). The '''TQA query API'''
 * (in the corresponding Scala package) has this knowledge across taxonomy documents. Hence we use this
 * taxonomy query API to query for specific types of concept declarations.
 *
 * Note that "substitution group declarations" for concepts are also returned as concept declarations!
 *
 * To understand this program, 3 topics need to be understood first:
 * <ul>
 * <li>The XML Schema topic of global element declarations and substitution groups</li>
 * <li>Basics of Core XBRL taxonomies, including DTSes (discoverable taxonomy sets)</li>
 * <li>Basics of XBRL Dimensions, in particular the different kinds of concept declarations, such as dimension declarations</li>
 * </ul>
 *
 * This program takes a taxonomy root directory, which is the local parent directory of local mirrors of
 * published taxonomies. So, for an www.nltaxonomie.nl taxonomy, the www.nltaxonomie.nl directory in the
 * local mirror is an immediate child folder of the local taxonomy root directory (there are no directories
 * for the protocol, like http or https). The second program argument is a DTS entrypoint schema URI
 * (as published, so probably an http scheme URI). More program arguments are allowed for additional
 * entrypoint URIs.
 *
 * To run this program, make sure to have a local root directory with all needed files for full DTS
 * discovery, including the www.xbrl.org and www.w3.org ones.
 *
 * @author Chris de Vreeze
 */
object ShowConceptSubstitutionGroups {

  private val logger = Logger.getGlobal

  def main(args: Array[String]): Unit = {
    require(args.size >= 2, s"Usage: ShowConceptSubstitutionGroups <taxo root dir> <entrypoint URI 1> ...")
    val rootDir = new File(args(0))
    require(rootDir.isDirectory, s"Not a directory: $rootDir")

    val entrypointUris = args.drop(1).map(u => URI.create(u)).toSet

    // Taxonomy bootstrapping, starting with a DocumentBuilder

    val processor = new Processor(false)

    val documentBuilder =
      new SaxonDocumentBuilder(processor.newDocumentBuilder(), UriResolvers.fromLocalMirrorRootDirectory(rootDir))

    val documentCollector: DocumentCollector = DefaultDtsCollector()

    val lenient = System.getProperty("lenient", "false").toBoolean

    val relationshipFactory =
      if (lenient) DefaultRelationshipFactory.LenientInstance else DefaultRelationshipFactory.StrictInstance

    // With the DocumentBuilder, DocumentCollector and RelationshipFactory, we can build a TaxonomyBase

    val taxoBuilder =
      TaxonomyBuilder.
        withDocumentBuilder(documentBuilder).
        withDocumentCollector(documentCollector).
        withRelationshipFactory(relationshipFactory)

    logger.info(s"Starting building the DTS with entrypoint(s) ${entrypointUris.mkString(", ")}")

    val basicTaxo = taxoBuilder.build(entrypointUris)

    val rootElems = basicTaxo.taxonomyBase.rootElems

    logger.info(s"The taxonomy has ${rootElems.size} taxonomy root elements")
    logger.info(s"The taxonomy has ${basicTaxo.relationships.size} relationships")

    // Retrieving different types of concept declarations

    logger.info(s"Retrieving different types of concept declarations")

    // First the hypercubes

    val hypercubeDecls = basicTaxo.findAllHypercubeDeclarations

    logger.info(s"Retrieving hypercube declarations. Substitution groups: ${getSubstitutionGroups(hypercubeDecls).mkString(", ")}")

    for (hypercubeDecl <- hypercubeDecls.groupBy(_.targetEName).mapValues(_.head).values.toSeq.sortBy(_.targetEName.toString)) {
      logger.info(s"Hypercube declaration. Name: ${hypercubeDecl.targetEName}. Substitution group: ${hypercubeDecl.globalElementDeclaration.substitutionGroupOption.get}.")
    }

    // Next the dimensions (first explicit, then typed)

    val explicitDimensionDecls = basicTaxo.findAllExplicitDimensionDeclarations

    logger.info(s"Retrieving explicit dimension declarations. Substitution groups: ${getSubstitutionGroups(explicitDimensionDecls).mkString(", ")}")

    for (dimensionDecl <- explicitDimensionDecls.groupBy(_.targetEName).mapValues(_.head).values.toSeq.sortBy(_.targetEName.toString)) {
      logger.info(s"Explicit dimension declaration. Name: ${dimensionDecl.targetEName}. Substitution group: ${dimensionDecl.globalElementDeclaration.substitutionGroupOption.get}.")
    }

    val typedDimensionDecls = basicTaxo.findAllTypedDimensionDeclarations

    logger.info(s"Retrieving typed dimension declarations. Substitution groups: ${getSubstitutionGroups(typedDimensionDecls).mkString(", ")}")

    for (dimensionDecl <- typedDimensionDecls.groupBy(_.targetEName).mapValues(_.head).values.toSeq.sortBy(_.targetEName.toString)) {
      logger.info(s"Typed dimension declaration. Name: ${dimensionDecl.targetEName}. Substitution group: ${dimensionDecl.globalElementDeclaration.substitutionGroupOption.get}.")
    }

    // Next the primary items (first abstract, then concrete)

    val primaryItemDecls = basicTaxo.findAllPrimaryItemDeclarations

    val (abstractPrimaryItemDecls, concretePrimaryItemDecls) = primaryItemDecls.partition(_.isAbstract)

    logger.info(s"Retrieving abstract primary item declarations. Substitution groups: ${getSubstitutionGroups(abstractPrimaryItemDecls).mkString(", ")}")

    for (itemDecl <- abstractPrimaryItemDecls.groupBy(_.targetEName).mapValues(_.head).values.toSeq.sortBy(_.targetEName.toString)) {
      logger.info(s"Abstract primary item declaration. Name: ${itemDecl.targetEName}. Substitution group: ${itemDecl.globalElementDeclaration.substitutionGroupOption.get}.")
    }

    logger.info(s"Retrieving concrete primary item declarations. Substitution groups: ${getSubstitutionGroups(concretePrimaryItemDecls).mkString(", ")}")

    for (itemDecl <- concretePrimaryItemDecls.groupBy(_.targetEName).mapValues(_.head).values.toSeq.sortBy(_.targetEName.toString)) {
      logger.info(s"Concrete primary item declaration. Name: ${itemDecl.targetEName}. Substitution group: ${itemDecl.globalElementDeclaration.substitutionGroupOption.get}.")
    }

    // Finally the tuples (first abstract, then concrete)

    val tupleDecls = basicTaxo.findAllTupleDeclarations

    val (abstractTupleDecls, concreteTupleDecls) = tupleDecls.partition(_.isAbstract)

    logger.info(s"Retrieving abstract tuple declarations. Substitution groups: ${getSubstitutionGroups(abstractTupleDecls).mkString(", ")}")

    for (tupleDecl <- abstractTupleDecls.groupBy(_.targetEName).mapValues(_.head).values.toSeq.sortBy(_.targetEName.toString)) {
      logger.info(s"Abstract tuple declaration. Name: ${tupleDecl.targetEName}. Substitution group: ${tupleDecl.globalElementDeclaration.substitutionGroupOption.get}.")
    }

    logger.info(s"Retrieving concrete tuple declarations. Substitution groups: ${getSubstitutionGroups(concreteTupleDecls).mkString(", ")}")

    for (tupleDecl <- concreteTupleDecls.groupBy(_.targetEName).mapValues(_.head).values.toSeq.sortBy(_.targetEName.toString)) {
      logger.info(s"Concrete tuple declaration. Name: ${tupleDecl.targetEName}. Substitution group: ${tupleDecl.globalElementDeclaration.substitutionGroupOption.get}.")
    }
  }

  private def getSubstitutionGroups(conceptDecls: immutable.IndexedSeq[ConceptDeclaration]): Set[EName] = {
    conceptDecls.flatMap(_.globalElementDeclaration.substitutionGroupOption).toSet
  }
}
