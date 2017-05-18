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

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.tqa.ENames.XLinkArcroleEName
import eu.cdevreeze.tqa.ENames.XLinkFromEName
import eu.cdevreeze.tqa.ENames.XLinkHrefEName
import eu.cdevreeze.tqa.ENames.XLinkLabelEName
import eu.cdevreeze.tqa.ENames.XLinkRoleEName
import eu.cdevreeze.tqa.ENames.XLinkToEName
import eu.cdevreeze.tqa.Namespaces.XLinkNamespace
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi

/**
 * An XLink element in an XBRL instance, obeying the constraints on XLink imposed by XBRL. For example, a XLink simple link or
 * a footnote link, which is an XLink extended link.
 *
 * XLink (see https://www.w3.org/TR/xlink11/) is a somewhat low level standard on top of XML, but it is
 * very important in an XBRL context. Some XBRL instance elements are also XLink elements.
 *
 * @author Chris de Vreeze
 */
sealed trait XLinkElem extends ScopedElemApi {

  def xlinkType: String

  def backingElem: BackingElemApi

  final def xlinkAttributes: Map[EName, String] = {
    resolvedAttributes.toMap.filterKeys(_.namespaceUriOption.contains(XLinkNamespace))
  }
}

/**
 * Simple or extended XLink link.
 */
sealed trait XLinkLink extends XLinkElem

/**
 * XLink child element of an extended link, so an XLink arc, locator or resource.
 */
sealed trait ChildXLink extends XLinkElem {

  final def elr: String = {
    underlyingParentElem.attribute(XLinkRoleEName)
  }

  final def underlyingParentElem: BackingElemApi = {
    backingElem.parent
  }
}

/**
 * XLink locator or resource.
 */
sealed trait LabeledXLink extends ChildXLink {

  final def xlinkLabel: String = {
    attribute(XLinkLabelEName)
  }
}

/**
 * XLink extended link. In an XBRL instance, that would be a footnote link.
 */
trait ExtendedLink extends XLinkLink {

  final def xlinkType: String = {
    "extended"
  }

  final def roleOption: Option[String] = {
    attributeOption(XLinkRoleEName)
  }

  final def xlinkChildren: immutable.IndexedSeq[ChildXLink] = {
    filterChildElems {
      case che: ChildXLink => true
      case che             => false
    } collect { case che: ChildXLink => che }
  }

  final def labeledXlinkChildren: immutable.IndexedSeq[LabeledXLink] = {
    filterChildElems {
      case che: LabeledXLink => true
      case che               => false
    } collect { case che: LabeledXLink => che }
  }

  final def arcs: immutable.IndexedSeq[XLinkArc] = {
    filterChildElems {
      case che: XLinkArc => true
      case che           => false
    } collect { case che: XLinkArc => che }
  }

  final def labeledXlinkMap: Map[String, immutable.IndexedSeq[LabeledXLink]] = {
    labeledXlinkChildren.groupBy(_.xlinkLabel)
  }
}

/**
 * XLink arc. In an XBRL instance, that would be an arc in a footnote link.
 *
 * The xlink:from and xlink:to attributes point to XLink locators or resources
 * in the same extended link with the corresponding xlink:label attributes.
 */
trait XLinkArc extends ChildXLink {

  final def xlinkType: String = {
    "arc"
  }

  final def arcrole: String = {
    attribute(XLinkArcroleEName)
  }

  final def from: String = {
    attribute(XLinkFromEName)
  }

  final def to: String = {
    attribute(XLinkToEName)
  }
}

/**
 * XLink resource. In an XBRL instance, that would be a resource in a footnote link.
 */
trait XLinkResource extends LabeledXLink {

  final def xlinkType: String = {
    "resource"
  }

  final def roleOption: Option[String] = {
    attributeOption(XLinkRoleEName)
  }
}

/**
 * XLink locator. In an XBRL instance, that would be a locator in a footnote link.
 */
trait XLinkLocator extends LabeledXLink {

  final def xlinkType: String = {
    "locator"
  }

  final def rawHref: URI = {
    URI.create(attribute(XLinkHrefEName))
  }
}

/**
 * XLink simple link. For example, a schemaRef in an XBRL instance.
 */
trait SimpleLink extends XLinkLink {

  final def xlinkType: String = {
    "simple"
  }

  final def rawHref: URI = {
    URI.create(attribute(XLinkHrefEName))
  }
}
