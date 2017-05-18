============
TQA Workshop
============

This workshop introduces the `TQA`_ (Taxonomy Query API) Scala library, along with corresponding XBRL taxonomy topics.

The following knowledge is assumed before starting with this workshop:

* Basic XML knowledge, about XML syntax, XML namespaces and XML Schema
* Some basic knowledge about XBRL taxonomies (schemas and linkbases) and XBRL instances
* Basic knowledge of Scala and its Collections API

TQA is gently introduced through the use of code examples and exercises. The exercises have the form of unit tests that
must be made to succeed.

XBRL taxonomies (from a TQA perspective) are treated from the bottom-up, starting with plain XML and ending
with taxonomy schemas and linkbases. For each of these XML and XBRL topics, the relevant parts of TQA and
the underlying `yaidom`_ API are treated.

.. _`TQA`: https://github.com/dvreeze/tqa
.. _`yaidom`: https://github.com/dvreeze/yaidom


Preparation
===========

Fork this (Scala SBT) project from Github, at `TQA Workshop`_. Make sure to either have an installation of `SBT`_ or
`Maven`_ available. Also make sure to have a Java JDK 8 installed.

Also download the API documentation of the TQA and yaidom libraries from `Maven central`_.

.. _`TQA Workshop`: https://github.com/dvreeze/tqa-workshop
.. _`SBT`: http://www.scala-sbt.org/download.html
.. _`Maven`: https://maven.apache.org/download.cgi
.. _`Maven central`: https://search.maven.org/


Chapter 1
=========

Chapter 1 introduces the **yaidom** XML querying (Scala) library. TQA is based on yaidom, so yaidom needs to be introduced
first.

First, make sure to have a good grasp of XML Namespaces. It may be advisable to read `Understanding Namespaces`_
to that end. After that, consider reading some `yaidom documentation`_, to familiarize yourself with the yaidom API.

Next, study program ``ShowXbrlInstanceElemAsMatrix`` in the source tree for chapter 1. It uses Scala class ``XbrlInstanceElemToRowsConverter``.
Study that class too, concentrating on its usage of yaidom query API calls (having the yaidom API documentation nearby).
Compile and run the ``ShowXbrlInstanceElemAsMatrix`` program.

Next, turn to the exercises. In the test source tree for chapter 1, fill in the missing parts in tests ``QuerySpec`` and
``QueryApiAsTheorySpec``, making the tests run successfully. This gives enough basic understanding of yaidom for the
following chapters.

It is important to not just make the tests compile and run successfully, but also to review how each test does what
the test description says it does. For example, there may be a test in which descendant-or-self elements of some element are
queried, where the query method itself has already been given, and where the missing piece to implement is only an
element predicate. It is not enough to implement this missing piece. It is also important to understand how the complete
test does what it says it does. This is true throughout the workshop, and not just for chapter 1!

After doing the exercises of chapter 1, the following should be clear:

* Yaidom offers a sound XML query API, interoperating well with the Scala Collections API, and respecting XML namespaces
* The yaidom ``BackingElemApi`` element query API abstraction can be backed by multiple different XML DOM-like implementations, such as Saxon

In the next chapters it will be shown that yaidom is not only flexible in the choice of the underlying XML DOM-like implementation,
but it is also a basis for creating arbitrary "yaidom dialects" on top of it, as "specific type-safe DOM trees". TQA is an
example of that.

Note that in comparison the Scala XML library does not offer this flexibility in supporting multiple "backends" and "dialects".
Neither does it enforce the creation of namespace-well-formed XML. For these reasons, yaidom had to be developed as an
alternative that is more suitable in domains like XBRL.

.. _`Understanding Namespaces`: http://www.lenzconsulting.com/namespaces/
.. _`yaidom documentation`: http://dvreeze.github.io/


Chapter 2
=========

Chapter 2 introduces **XLink**, **XML Base** and **XPointer**, from the perspective of yaidom or TQA. Yaidom knows about XML Base,
but it does not know anything about XLink and XPointer. A little bit of TQA is introduced, namely the "type-safe DOM
abstractions" for XLink content, and TQA's support for XPointer processing (as applied in an XBRL context).

XLink is everywhere in XBRL. XML Base and (element scheme) XPointer are not. On the other hand, XLink processing in
an XBRL context must be aware of XML Base and (element scheme or shorthand) XPointer, so it makes sense to create some
awareness about that in this chapter.

The assumed familiarity with XBRL taxonomies on the part of the reader implies some knowledge about XLink, since XBRL
linkbases make no sense without any knowledge about XLink. If so desired, have a look at this `XLink extended link`_
tutorial. This `XML Base`_ tutorial is also very clear while taking little time to read. The use of XPointer in an
XBRL context is restricted to shorthand pointers (the familiar ID fragments in URIs that you may not even think of as
being XPointer) and element scheme pointers. For the latter, see the short `element scheme XPointer`_ specification.

Next, turn to the exercises. In the test source tree for chapter 2, fill in the missing parts in tests ``XLinkSpec``,
``XMLBaseSpec`` and ``XPointerSpec``, making the tests run successfully. This gives enough basic understanding
of XBRL XLink processing (with awareness of XML Base and XPointer) for the following chapters.

After doing the exercises of chapter 2, the following should be clear:

* TQA and yaidom support XLink processing (as used in linkbases in XBRL taxonomies)
* This XLink processing respects XML Base and (shorthand and element scheme) XPointer
* XLink content in taxonomy documents has been modeled in TQA as part of an "XBRL yaidom dialect" on top of yaidom
* For the API users this means a "yaidom-like" development experience, be it a more type-safe and (XBRL taxonomy) domain-specific one

.. _`XLink extended link`: http://zvon.org/xxl/xlink/xlink_extend/OutputExamples/frame_xlinkextend_html.html
.. _`XML Base`: http://zvon.org/xxl/XMLBaseTutorial/Output/
.. _`element scheme XPointer`: https://www.w3.org/TR/xptr-element/


Chapter 3
=========

Chapter 3 introduces **XBRL instances**, as seen from the perspective of a **"yaidom dialect"** for XBRL instances.
This custom yaidom-like XBRL instance query API is used throughout the remainder of the course for XBRL instance processing.
It should be noted that the "dialect" looks no further than the instance document itself. To check its validity, for
example, the taxonomy used by the instance must be available.

First study program ``ShowXbrlInstanceAsMatrix`` in the source tree for chapter 3. It uses Scala class ``XbrlInstanceToRowsConverter``.
Study that class too, concentrating on its usage of "XBRL yaidom dialect" query API calls. Compile and run the
``ShowXbrlInstanceAsMatrix`` program. Note how compared to the corresponding code for chapter 1, the code size shrinks
and becomes more type-safe. Apparently the investment in the development of a "yaidom dialect" for instances pays back
when using it. The same holds for TQA and taxonomy content.

Next, turn to the exercise. In the test source tree for chapter 3, fill in the missing parts in test ``QuerySpec``,
making the test run successfully. This gives enough basic understanding of custom "yaidom dialects" for the following chapters.

After doing the exercise of chapter 3, the following should be clear:

* Custom "yaidom dialects" (for XBRL instances or taxonomies, or in other domains) improve the development experience compared to "raw yaidom"
* They typically contain many classes and easy to use methods for specific types of content in the domain modeled
* It is easy to fall back on "type-safe yaidom query methods" (offered by the yaidom ``SubtypeAwareElemApi`` trait) where needed
* If needed, it is easy to fall back on regular yaidom query API methods

This is true for both the "dialect" for XBRL instances (used in many exercises) and TQA.
