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

package eu.cdevreeze.tqaworkshop.xbrlinstance

import java.time.LocalDate
import java.time.LocalDateTime

import scala.collection.immutable
import scala.reflect.classTag

import XbrliElem._
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.ElemApi.anyElem
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike

/**
 * XML element inside XBRL instance (or the entire XBRL instance itself). This API is immutable, provided
 * the backing element is immutable.
 *
 * The yaidom `SubtypeAwareElemApi` and `ScopedElemApi` query API is offered.
 *
 * Also note that the package-private constructor contains redundant data, in order to speed up (yaidom-based) querying.
 *
 * These XBRL instance elements are just an XBRL instance view on the underlying "backing element" tree, and
 * therefore do not know about the taxonomy describing the XBRL instance (other than the href to the DTS entrypoint).
 * It is not even required that the XBRL instance is schema-valid. Construction of an instance is indeed quite lenient.
 *
 * As a consequence, this model must recognize facts by only looking at the elements and their ancestry, without knowing
 * anything about the substitution groups of the corresponding concept declarations. Fortunately, the XBRL instance
 * schema (xbrl-instance-2003-12-31.xsd) and the specification of allowed XBRL tuple content are (almost) restrictive enough
 * in order to recognize facts.
 *
 * It is even possible to easily distinguish between item facts and tuple facts, based on the presence or absence of the
 * contextRef attribute. There is one complication, though, and that is nil item and tuple facts. Unfortunately, concept
 * declarations in taxonomy schemas may have the nillable attribute set to true. This led to some clutter in the
 * inheritance hierarchy for numeric item facts.
 *
 * Hence, regarding nil facts, the user of the API is responsible for keeping in mind that facts can indeed be nil facts
 * (which facts are easy to filter away).
 *
 * Another limitation is that without the taxonomy, default dimensions are unknown. Finally, the lack of typing information
 * is a limitation.
 *
 * Note that the backing element implementation can be any implementation of yaidom query API trait `BackingElemApi`.
 *
 * This class hierarchy depends on Java 8 or later, due to the use of Java 8 time API.
 *
 * @author Chris de Vreeze
 */
sealed class XbrliElem private[xbrlinstance] (
    val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends ScopedElemLike with SubtypeAwareElemLike {

  require(childElems.map(_.backingElem) == backingElem.findAllChildElems)

  type ThisElem = XbrliElem

  final def thisElem: ThisElem = this

  /**
   * Very fast implementation of findAllChildElems, for fast querying
   */
  final def findAllChildElems: immutable.IndexedSeq[XbrliElem] = childElems

  final def resolvedName: EName = backingElem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] = backingElem.resolvedAttributes

  final def qname: QName = backingElem.qname

  final def attributes: immutable.Iterable[(QName, String)] = backingElem.attributes

  final def scope: Scope = backingElem.scope

  final def text: String = backingElem.text

  final override def equals(other: Any): Boolean = other match {
    case e: XbrliElem => backingElem == e.backingElem
    case _            => false
  }

  final override def hashCode: Int = backingElem.hashCode
}

/**
 * XBRL instance.
 *
 * It does not check validity of the XBRL instance. Neither does it know about the DTS describing the XBRL instance.
 * It does, however, contain the entrypoint URI(s) to the DTS.
 *
 * Without any knowledge about the DTS, this class only recognizes (item and tuple) facts by looking at the
 * structure of the element and its ancestry. Attribute @contextRef is only allowed for item facts, and tuple facts can be
 * recognized by looking at the "path" of the element.
 *
 * @author Chris de Vreeze
 */
final class XbrlInstance private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliXbrlEName, s"Expected EName $XbrliXbrlEName but found $resolvedName")
  require(backingElem.path.isEmpty, s"The XbrlInstance must be the root element")

  val allContexts: immutable.IndexedSeq[XbrliContext] =
    findAllChildElemsOfType(classTag[XbrliContext])

  val allContextsById: Map[String, XbrliContext] =
    allContexts.groupBy(_.id) mapValues (_.head)

  val allUnits: immutable.IndexedSeq[XbrliUnit] =
    findAllChildElemsOfType(classTag[XbrliUnit])

  val allUnitsById: Map[String, XbrliUnit] =
    allUnits.groupBy(_.id) mapValues (_.head)

  val allTopLevelFacts: immutable.IndexedSeq[Fact] =
    findAllChildElemsOfType(classTag[Fact])

  val allTopLevelItems: immutable.IndexedSeq[ItemFact] =
    findAllChildElemsOfType(classTag[ItemFact])

  val allTopLevelTuples: immutable.IndexedSeq[TupleFact] =
    findAllChildElemsOfType(classTag[TupleFact])

  val allTopLevelFactsByEName: Map[EName, immutable.IndexedSeq[Fact]] =
    allTopLevelFacts groupBy (_.resolvedName)

  val allTopLevelItemsByEName: Map[EName, immutable.IndexedSeq[ItemFact]] =
    allTopLevelItems groupBy (_.resolvedName)

  val allTopLevelTuplesByEName: Map[EName, immutable.IndexedSeq[TupleFact]] =
    allTopLevelTuples groupBy (_.resolvedName)

  def filterContexts(p: XbrliContext => Boolean): immutable.IndexedSeq[XbrliContext] = {
    filterChildElemsOfType(classTag[XbrliContext])(p)
  }

  def filterUnits(p: XbrliUnit => Boolean): immutable.IndexedSeq[XbrliUnit] = {
    filterChildElemsOfType(classTag[XbrliUnit])(p)
  }

  def filterTopLevelFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
    filterChildElemsOfType(classTag[Fact])(p)
  }

  def filterTopLevelItems(p: ItemFact => Boolean): immutable.IndexedSeq[ItemFact] = {
    filterChildElemsOfType(classTag[ItemFact])(p)
  }

  def filterTopLevelTuples(p: TupleFact => Boolean): immutable.IndexedSeq[TupleFact] = {
    filterChildElemsOfType(classTag[TupleFact])(p)
  }

  def findAllFacts: immutable.IndexedSeq[Fact] = {
    findAllElemsOfType(classTag[Fact])
  }

  def findAllItems: immutable.IndexedSeq[ItemFact] = {
    findAllElemsOfType(classTag[ItemFact])
  }

  def findAllTuples: immutable.IndexedSeq[TupleFact] = {
    findAllElemsOfType(classTag[TupleFact])
  }

  def filterFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
    filterElemsOfType(classTag[Fact])(p)
  }

  def filterItems(p: ItemFact => Boolean): immutable.IndexedSeq[ItemFact] = {
    filterElemsOfType(classTag[ItemFact])(p)
  }

  def filterTuples(p: TupleFact => Boolean): immutable.IndexedSeq[TupleFact] = {
    filterElemsOfType(classTag[TupleFact])(p)
  }

  def findAllSchemaRefs: immutable.IndexedSeq[SchemaRef] = {
    findAllChildElemsOfType(classTag[SchemaRef])
  }

  def findAllLinkbaseRefs: immutable.IndexedSeq[LinkbaseRef] = {
    findAllChildElemsOfType(classTag[LinkbaseRef])
  }

  def findAllRoleRefs: immutable.IndexedSeq[RoleRef] = {
    findAllChildElemsOfType(classTag[RoleRef])
  }

  def findAllArcroleRefs: immutable.IndexedSeq[ArcroleRef] = {
    findAllChildElemsOfType(classTag[ArcroleRef])
  }

  def findAllFootnoteLinks: immutable.IndexedSeq[FootnoteLink] = {
    findAllChildElemsOfType(classTag[FootnoteLink])
  }
}

/**
 * SchemaRef in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class SchemaRef private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with SimpleLink {

  require(resolvedName == LinkSchemaRefEName, s"Expected EName $LinkSchemaRefEName but found $resolvedName")
}

/**
 * LinkbaseRef in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class LinkbaseRef private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with SimpleLink {

  require(resolvedName == LinkLinkbaseRefEName, s"Expected EName $LinkLinkbaseRefEName but found $resolvedName")
}

/**
 * RoleRef in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class RoleRef private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with SimpleLink {

  require(resolvedName == LinkRoleRefEName, s"Expected EName $LinkRoleRefEName but found $resolvedName")
}

/**
 * ArcroleRef in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class ArcroleRef private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with SimpleLink {

  require(resolvedName == LinkArcroleRefEName, s"Expected EName $LinkArcroleRefEName but found $resolvedName")
}

/**
 * Context in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class XbrliContext private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliContextEName, s"Expected EName $XbrliContextEName but found $resolvedName")

  def id: String = attribute(IdEName)

  def entity: Entity = {
    getChildElemOfType(classTag[Entity])(anyElem)
  }

  def period: Period = {
    getChildElemOfType(classTag[Period])(anyElem)
  }

  def scenarioOption: Option[Scenario] = {
    findChildElemOfType(classTag[Scenario])(anyElem)
  }

  def identifierScheme: String = {
    entity.identifierScheme
  }

  def identifierValue: String = {
    entity.identifierValue
  }

  def explicitDimensionMembers: Map[EName, EName] = {
    entity.segmentOption.map(_.explicitDimensionMembers).getOrElse(Map.empty) ++
      scenarioOption.map(_.explicitDimensionMembers).getOrElse(Map.empty)
  }
}

/**
 * Unit in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class XbrliUnit private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliUnitEName, s"Expected EName $XbrliUnitEName but found $resolvedName")

  def id: String = attribute(IdEName)

  def measures: immutable.IndexedSeq[EName] = {
    filterChildElems(XbrliMeasureEName) map (e => e.textAsResolvedQName)
  }

  def divide: Divide = {
    getChildElemOfType(classTag[Divide])(anyElem)
  }

  // TODO Lists of numerators and denominators
}

/**
 * Item or tuple fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
 *
 * @author Chris de Vreeze
 */
abstract class Fact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  final def isTopLevel: Boolean = path.entries.size == 1

  final def isNil: Boolean = attributeOption(XsiNilEName).contains("true")

  /**
   * In the aspect model for instances, the concept core aspect.
   */
  final def conceptEName: EName = resolvedName

  final def path: Path = backingElem.path
}

/**
 * Item fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
 *
 * @author Chris de Vreeze
 */
abstract class ItemFact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends Fact(backingElem, childElems) {

  require(attributeOption(ContextRefEName).isDefined, s"Expected attribute $ContextRefEName")

  final def contextRef: String = attribute(ContextRefEName)

  def unitRefOption: Option[String]
}

/**
 * Non-numeric item fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
 *
 * @author Chris de Vreeze
 */
final class NonNumericItemFact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends ItemFact(backingElem, childElems) {

  require(attributeOption(UnitRefEName).isEmpty, s"Expected no attribute $UnitRefEName")

  def unitRefOption: Option[String] = None
}

/**
 * Numeric item fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
 *
 * @author Chris de Vreeze
 */
abstract class NumericItemFact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends ItemFact(backingElem, childElems) {

  require(attributeOption(UnitRefEName).isDefined, s"Expected attribute $UnitRefEName")

  final def unitRef: String = attribute(UnitRefEName)

  final def unitRefOption: Option[String] = Some(unitRef)
}

/**
 * Nil numeric item fact in an XBRL instance, either top-level or nested
 *
 * @author Chris de Vreeze
 */
final class NilNumericItemFact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends NumericItemFact(backingElem, childElems) {

  require(isNil, s"Expected nil numeric item fact")
}

/**
 * Non-nil non-fraction numeric item fact in an XBRL instance, either top-level or nested
 *
 * @author Chris de Vreeze
 */
final class NonNilNonFractionNumericItemFact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends NumericItemFact(backingElem, childElems) {

  require(!isNil, s"Expected non-nil numeric item fact")

  def precisionOption: Option[String] = attributeOption(PrecisionEName)

  def decimalsOption: Option[String] = attributeOption(DecimalsEName)
}

/**
 * Non-nil fraction item fact in an XBRL instance, either top-level or nested
 *
 * @author Chris de Vreeze
 */
final class NonNilFractionItemFact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends NumericItemFact(backingElem, childElems) {

  require(!isNil, s"Expected non-nil numeric item fact")

  require(findAllChildElems.map(_.resolvedName).toSet == Set(XbrliNumeratorEName, XbrliDenominatorEName))

  def numerator: BigDecimal = {
    val s = getChildElem(XbrliNumeratorEName).text
    BigDecimal(s)
  }

  def denominator: BigDecimal = {
    val s = getChildElem(XbrliDenominatorEName).text
    BigDecimal(s)
  }
}

/**
 * Tuple fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
 *
 * @author Chris de Vreeze
 */
final class TupleFact private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends Fact(backingElem, childElems) {

  def findAllChildFacts: immutable.IndexedSeq[Fact] = {
    findAllChildElemsOfType(classTag[Fact])
  }

  def findAllFacts: immutable.IndexedSeq[Fact] = {
    findAllElemsOfType(classTag[Fact])
  }

  def filterChildFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
    filterChildElemsOfType(classTag[Fact])(p)
  }

  def filterFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
    filterElemsOfType(classTag[Fact])(p)
  }
}

/**
 * FootnoteLink in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class FootnoteLink private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with ExtendedLink {

  require(resolvedName == LinkFootnoteLinkEName, s"Expected EName $LinkFootnoteLinkEName but found $resolvedName")
}

/**
 * FootnoteArc in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class FootnoteArc private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with XLinkArc {

  require(resolvedName == LinkFootnoteArcEName, s"Expected EName $LinkFootnoteArcEName but found $resolvedName")
}

/**
 * Footnote in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class Footnote private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with XLinkResource {

  require(resolvedName == LinkFootnoteEName, s"Expected EName $LinkFootnoteEName but found $resolvedName")
}

/**
 * Standard locator in an XBRL instance
 *
 * @author Chris de Vreeze
 */
final class StandardLoc private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with XLinkLocator {

  require(resolvedName == LinkLocEName, s"Expected EName $LinkLocEName but found $resolvedName")
}

/**
 * Entity in an XBRL instance context
 *
 * @author Chris de Vreeze
 */
final class Entity private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliEntityEName, s"Expected EName $XbrliEntityEName but found $resolvedName")

  def identifier: Identifier = {
    getChildElemOfType(classTag[Identifier])(anyElem)
  }

  def segmentOption: Option[Segment] = {
    findChildElemOfType(classTag[Segment])(anyElem)
  }

  def identifierScheme: String = {
    identifier.identifierScheme
  }

  def identifierValue: String = {
    identifier.identifierValue
  }
}

/**
 * Period in an XBRL instance context
 *
 * @author Chris de Vreeze
 */
abstract class Period private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliPeriodEName, s"Expected EName $XbrliPeriodEName but found $resolvedName")

  def isInstantPeriod: Boolean = {
    findChildElemOfType(classTag[Instant])(anyElem).isDefined
  }

  def isStartEndDatePeriod: Boolean = {
    findChildElemOfType(classTag[StartDate])(anyElem).isDefined
  }

  def isForeverPeriod: Boolean = {
    findChildElemOfType(classTag[Forever])(anyElem).isDefined
  }

  def asInstantPeriod: InstantPeriod = {
    require(isInstantPeriod, s"Not an instant period: $backingElem")
    this.asInstanceOf[InstantPeriod]
  }

  def asStartEndDatePeriod: StartEndDatePeriod = {
    require(isStartEndDatePeriod, s"Not a finite duration period: $backingElem")
    this.asInstanceOf[StartEndDatePeriod]
  }

  def asForeverPeriod: ForeverPeriod = {
    require(isForeverPeriod, s"Not a forever period: $backingElem")
    this.asInstanceOf[ForeverPeriod]
  }
}

/**
 * Instant period in an XBRL instance context
 *
 * @author Chris de Vreeze
 */
final class InstantPeriod private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends Period(backingElem, childElems) {

  require(isInstantPeriod)

  // TODO How about time zones?

  def instant: Instant = getChildElemOfType(classTag[Instant])(anyElem)

  def instantDateTime: LocalDateTime = instant.dateTime
}

/**
 * Start-end-date period in an XBRL instance context
 *
 * @author Chris de Vreeze
 */
final class StartEndDatePeriod private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends Period(backingElem, childElems) {

  require(isStartEndDatePeriod)

  // TODO How about time zones?

  def startDate: StartDate = getChildElemOfType(classTag[StartDate])(anyElem)

  def endDate: EndDate = getChildElemOfType(classTag[EndDate])(anyElem)

  def startDateTime: LocalDateTime = startDate.dateTime

  def endDateTime: LocalDateTime = endDate.dateTime
}

/**
 * Forever period in an XBRL instance context
 *
 * @author Chris de Vreeze
 */
final class ForeverPeriod private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends Period(backingElem, childElems) {

  require(isForeverPeriod)
}

/**
 * Instant in an XBRL instance context's period
 *
 * @author Chris de Vreeze
 */
final class Instant private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliInstantEName, s"Expected EName $XbrliInstantEName but found $resolvedName")

  def dateTime: LocalDateTime = {
    Period.parseInstantOrEndDate(text)
  }
}

/**
 * Start date in an XBRL instance context's period
 *
 * @author Chris de Vreeze
 */
final class StartDate private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliStartDateEName, s"Expected EName $XbrliStartDateEName but found $resolvedName")

  def dateTime: LocalDateTime = {
    Period.parseStartDate(text)
  }
}

/**
 * End date in an XBRL instance context's period
 *
 * @author Chris de Vreeze
 */
final class EndDate private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliEndDateEName, s"Expected EName $XbrliEndDateEName but found $resolvedName")

  def dateTime: LocalDateTime = {
    Period.parseInstantOrEndDate(text)
  }
}

/**
 * Forver in an XBRL instance context's period
 *
 * @author Chris de Vreeze
 */
final class Forever private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliForeverEName, s"Expected EName $XbrliForeverEName but found $resolvedName")
}

sealed trait MayContainDimensions extends XbrliElem {

  final def explicitMembers: immutable.IndexedSeq[ExplicitMember] = {
    findAllChildElemsOfType(classTag[ExplicitMember])
  }

  final def explicitDimensionMembers: Map[EName, EName] = {
    (explicitMembers map { e =>
      val dim = e.attributeAsResolvedQName(DimensionEName)
      val mem = e.textAsResolvedQName

      (dim -> mem)
    }).toMap
  }

  // TODO Typed dimension members
}

/**
 * Scenario in an XBRL instance context
 *
 * @author Chris de Vreeze
 */
final class Scenario private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with MayContainDimensions {

  require(resolvedName == XbrliScenarioEName, s"Expected EName $XbrliScenarioEName but found $resolvedName")
}

/**
 * Segment in an XBRL instance context entity
 *
 * @author Chris de Vreeze
 */
final class Segment private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) with MayContainDimensions {

  require(resolvedName == XbrliSegmentEName, s"Expected EName $XbrliSegmentEName but found $resolvedName")
}

/**
 * Identifier in an XBRL instance context entity
 *
 * @author Chris de Vreeze
 */
final class Identifier private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliIdentifierEName, s"Expected EName $XbrliIdentifierEName but found $resolvedName")

  def identifierScheme: String = attribute(SchemeEName)

  def identifierValue: String = text
}

/**
 * Divide in an XBRL instance unit
 *
 * @author Chris de Vreeze
 */
final class Divide private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrliDivideEName, s"Expected EName $XbrliDivideEName but found $resolvedName")

  def numerator: immutable.IndexedSeq[EName] = {
    val unitNumerator = getChildElem(XbrliUnitNumeratorEName)
    val result = unitNumerator.filterChildElems(XbrliMeasureEName).map(e => e.textAsResolvedQName)
    result
  }

  def denominator: immutable.IndexedSeq[EName] = {
    val unitDenominator = getChildElem(XbrliUnitDenominatorEName)
    val result = unitDenominator.filterChildElems(XbrliMeasureEName).map(e => e.textAsResolvedQName)
    result
  }
}

final class ExplicitMember private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrldiExplicitMemberEName, s"Expected EName $XbrldiExplicitMemberEName but found $resolvedName")

  def dimension: EName = {
    attributeAsResolvedQName(DimensionEName)
  }

  def member: EName = {
    textAsResolvedQName
  }
}

final class TypedMember private[xbrlinstance] (
    override val backingElem: BackingElemApi,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(backingElem, childElems) {

  require(resolvedName == XbrldiTypedMemberEName, s"Expected EName $XbrldiTypedMemberEName but found $resolvedName")

  def dimension: EName = {
    attributeAsResolvedQName(DimensionEName)
  }
}

object XbrliElem {

  val XbrliNs = "http://www.xbrl.org/2003/instance"
  val LinkNs = "http://www.xbrl.org/2003/linkbase"
  val XmlNs = "http://www.w3.org/XML/1998/namespace"
  val XsiNs = "http://www.w3.org/2001/XMLSchema-instance"
  val XbrldiNs = "http://xbrl.org/2006/xbrldi"

  val XbrliXbrlEName = EName(XbrliNs, "xbrl")
  val XbrliContextEName = EName(XbrliNs, "context")
  val XbrliUnitEName = EName(XbrliNs, "unit")
  val XbrliEntityEName = EName(XbrliNs, "entity")
  val XbrliPeriodEName = EName(XbrliNs, "period")
  val XbrliScenarioEName = EName(XbrliNs, "scenario")
  val XbrliIdentifierEName = EName(XbrliNs, "identifier")
  val XbrliSegmentEName = EName(XbrliNs, "segment")
  val XbrliInstantEName = EName(XbrliNs, "instant")
  val XbrliStartDateEName = EName(XbrliNs, "startDate")
  val XbrliEndDateEName = EName(XbrliNs, "endDate")
  val XbrliForeverEName = EName(XbrliNs, "forever")
  val XbrliMeasureEName = EName(XbrliNs, "measure")
  val XbrliDivideEName = EName(XbrliNs, "divide")
  val XbrliNumeratorEName = EName(XbrliNs, "numerator")
  val XbrliDenominatorEName = EName(XbrliNs, "denominator")
  val XbrliUnitNumeratorEName = EName(XbrliNs, "unitNumerator")
  val XbrliUnitDenominatorEName = EName(XbrliNs, "unitDenominator")

  val XbrldiExplicitMemberEName = EName(XbrldiNs, "explicitMember")
  val XbrldiTypedMemberEName = EName(XbrldiNs, "typedMember")

  val LinkSchemaRefEName = EName(LinkNs, "schemaRef")
  val LinkLinkbaseRefEName = EName(LinkNs, "linkbaseRef")
  val LinkRoleRefEName = EName(LinkNs, "roleRef")
  val LinkArcroleRefEName = EName(LinkNs, "arcroleRef")
  val LinkFootnoteLinkEName = EName(LinkNs, "footnoteLink")
  val LinkFootnoteArcEName = EName(LinkNs, "footnoteArc")
  val LinkFootnoteEName = EName(LinkNs, "footnote")
  val LinkLocEName = EName(LinkNs, "loc")

  val XmlLangEName = EName(XmlNs, "lang")

  val XsiNilEName = EName(XsiNs, "nil")

  val IdEName = EName("id")
  val ContextRefEName = EName("contextRef")
  val UnitRefEName = EName("unitRef")
  val PrecisionEName = EName("precision")
  val DecimalsEName = EName("decimals")
  val SchemeEName = EName("scheme")
  val DimensionEName = EName("dimension")

  /**
   * Expensive method to create an XbrliElem tree
   */
  def apply(elem: BackingElemApi): XbrliElem = {
    // Recursive calls
    val childElems = elem.findAllChildElems.map(e => apply(e))
    apply(elem, childElems)
  }

  private[xbrlinstance] def apply(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): XbrliElem = {
    elem.resolvedName.namespaceUriOption match {
      case Some(XbrliNs)  => applyForXbrliNamespace(elem, childElems)
      case Some(LinkNs)   => applyForLinkNamespace(elem, childElems)
      case Some(XbrldiNs) => applyForXbrldiNamespace(elem, childElems)
      case _              => applyForOtherNamespace(elem, childElems)
    }
  }

  private[xbrlinstance] def applyForXbrliNamespace(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): XbrliElem = {
    elem.resolvedName match {
      case XbrliXbrlEName                           => new XbrlInstance(elem, childElems)
      case XbrliContextEName                        => new XbrliContext(elem, childElems)
      case XbrliUnitEName                           => new XbrliUnit(elem, childElems)
      case XbrliEntityEName                         => new Entity(elem, childElems)
      case XbrliPeriodEName if Period.accepts(elem) => Period(elem, childElems)
      case XbrliScenarioEName                       => new Scenario(elem, childElems)
      case XbrliSegmentEName                        => new Segment(elem, childElems)
      case XbrliIdentifierEName                     => new Identifier(elem, childElems)
      case XbrliDivideEName                         => new Divide(elem, childElems)
      case XbrliInstantEName                        => new Instant(elem, childElems)
      case XbrliStartDateEName                      => new StartDate(elem, childElems)
      case XbrliEndDateEName                        => new EndDate(elem, childElems)
      case XbrliForeverEName                        => new Forever(elem, childElems)
      case _                                        => new XbrliElem(elem, childElems)
    }
  }

  private[xbrlinstance] def applyForLinkNamespace(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): XbrliElem = {
    elem.resolvedName match {
      case LinkSchemaRefEName    => new SchemaRef(elem, childElems)
      case LinkLinkbaseRefEName  => new LinkbaseRef(elem, childElems)
      case LinkRoleRefEName      => new RoleRef(elem, childElems)
      case LinkArcroleRefEName   => new ArcroleRef(elem, childElems)
      case LinkFootnoteLinkEName => new FootnoteLink(elem, childElems)
      case LinkFootnoteArcEName  => new FootnoteArc(elem, childElems)
      case LinkFootnoteEName     => new Footnote(elem, childElems)
      case LinkLocEName          => new StandardLoc(elem, childElems)
      case _                     => new XbrliElem(elem, childElems)
    }
  }

  private[xbrlinstance] def applyForXbrldiNamespace(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): XbrliElem = {
    elem.resolvedName match {
      case XbrldiExplicitMemberEName => new ExplicitMember(elem, childElems)
      case XbrldiTypedMemberEName    => new TypedMember(elem, childElems)
      case _                         => new XbrliElem(elem, childElems)
    }
  }

  private[xbrlinstance] def applyForOtherNamespace(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): XbrliElem = {
    elem.resolvedName match {
      case _ if Fact.accepts(elem) => Fact(elem, childElems)
      case _                       => new XbrliElem(elem, childElems)
    }
  }
}

object XbrlInstance {

  def apply(elem: BackingElemApi): XbrlInstance = {
    require(elem.resolvedName == XbrliXbrlEName)
    XbrliElem.apply(elem).asInstanceOf[XbrlInstance]
  }
}

object Period {

  def accepts(elem: BackingElemApi): Boolean = {
    elem.resolvedName == XbrliPeriodEName && (isInstant(elem) || isFiniteDuration(elem) || isForever(elem))
  }

  private[xbrlinstance] def apply(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): Period = {
    if (isInstant(elem)) new InstantPeriod(elem, childElems)
    else if (isFiniteDuration(elem)) new StartEndDatePeriod(elem, childElems)
    else new ForeverPeriod(elem, childElems)
  }

  private def isInstant(elem: BackingElemApi): Boolean = {
    elem.findChildElem(XbrliInstantEName).isDefined
  }

  private def isFiniteDuration(elem: BackingElemApi): Boolean = {
    elem.findChildElem(XbrliStartDateEName).isDefined
  }

  private def isForever(elem: BackingElemApi): Boolean = {
    elem.findChildElem(XbrliForeverEName).isDefined
  }

  private[xbrlinstance] def parseStartDate(s: String): LocalDateTime = {
    if (s.contains('T')) {
      LocalDateTime.parse(s)
    } else {
      LocalDate.parse(s).atStartOfDay
    }
  }

  private[xbrlinstance] def parseInstantOrEndDate(s: String): LocalDateTime = {
    if (s.contains('T')) {
      LocalDateTime.parse(s)
    } else {
      LocalDate.parse(s).plusDays(1).atStartOfDay
    }
  }
}

object Fact {

  def accepts(elem: BackingElemApi): Boolean = ItemFact.accepts(elem) || TupleFact.accepts(elem)

  private[xbrlinstance] def apply(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): Fact =
    if (ItemFact.accepts(elem)) ItemFact(elem, childElems) else TupleFact(elem, childElems)

  def isFactPath(path: Path): Boolean = {
    !path.isEmpty &&
      !Set(Option(LinkNs), Option(XbrliNs)).contains(path.firstEntry.elementName.namespaceUriOption)
  }
}

object ItemFact {

  def accepts(elem: BackingElemApi): Boolean = {
    Fact.isFactPath(elem.path) &&
      elem.attributeOption(ContextRefEName).isDefined
  }

  private[xbrlinstance] def apply(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): ItemFact = {
    require(Fact.isFactPath(elem.path))
    require(elem.attributeOption(ContextRefEName).isDefined)

    val unitRefOption = elem.attributeOption(UnitRefEName)

    if (unitRefOption.isEmpty) new NonNumericItemFact(elem, childElems)
    else {
      if (elem.attributeOption(XsiNilEName).contains("true"))
        new NilNumericItemFact(elem, childElems)
      else if (elem.findChildElem(withEName(XbrliNumeratorEName)).isDefined)
        new NonNilFractionItemFact(elem, childElems)
      else
        new NonNilNonFractionNumericItemFact(elem, childElems)
    }
  }
}

object TupleFact {

  def accepts(elem: BackingElemApi): Boolean = {
    Fact.isFactPath(elem.path) &&
      elem.attributeOption(ContextRefEName).isEmpty
  }

  private[xbrlinstance] def apply(elem: BackingElemApi, childElems: immutable.IndexedSeq[XbrliElem]): TupleFact = {
    require(Fact.isFactPath(elem.path))
    require(elem.attributeOption(ContextRefEName).isEmpty)

    new TupleFact(elem, childElems)
  }
}
