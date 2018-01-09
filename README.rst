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

XBRL taxonomies (from a TQA perspective) are treated from the bottom-up, starting with plain XML, proceeding
with taxonomy schemas and linkbases, and ending with topics like dimensions and performance considerations.
For each of these XML and XBRL topics, the relevant parts of TQA and the underlying `yaidom`_ API are treated.

.. _`TQA`: https://github.com/dvreeze/tqa
.. _`yaidom`: https://github.com/dvreeze/yaidom


Preparation
===========

Fork this (Scala SBT) project from Github, at `TQA Workshop`_. Make sure to either have an installation of `SBT`_ or
`Maven`_ available. Also make sure to have a Java JDK 8 installed.

Also download the API documentation of the TQA and yaidom libraries from `Maven central`_. Alternatively,
bookmark the `yaidom API documentation`_ and `TQA API documentation`_ at `javadoc.io`_.

During the workshop, the **yaidom and TQA API documentation** will be needed extensively, so it is advisable to
have this documentation at your disposal in a browser.

.. _`TQA Workshop`: https://github.com/dvreeze/tqa-workshop
.. _`SBT`: http://www.scala-sbt.org/download.html
.. _`Maven`: https://maven.apache.org/download.cgi
.. _`Maven central`: https://search.maven.org/
.. _`yaidom API documentation`: https://www.javadoc.io/doc/eu.cdevreeze.yaidom/yaidom_2.12/1.7.0
.. _`TQA API documentation`: https://www.javadoc.io/doc/eu.cdevreeze.tqa/tqa_2.12/0.6.1
.. _`javadoc.io`: http://javadoc.io/


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

Next, turn to the exercises. In the test source tree for chapter 3, fill in the missing parts in test ``QuerySpec``,
making the test run successfully. This gives enough basic understanding of custom "yaidom dialects" for the following chapters.

After doing the exercises of chapter 3, the following should be clear:

* Custom "yaidom dialects" (for XBRL instances or taxonomies, or in other domains) improve the development experience compared to "raw yaidom"
* They typically contain many classes and easy to use methods for specific types of content in the domain modeled
* It is easy to fall back on "type-safe yaidom query methods" (offered by the yaidom ``SubtypeAwareElemApi`` trait) where needed
* If needed, it is easy to fall back on regular yaidom query API methods

This is true for both the "dialect" for XBRL instances (used in many exercises) and TQA.


Chapter 4
=========

Chapter 4 introduces **XBRL taxonomy schemas**, from the perspective of TQA. XBRL taxonomies are made up of
schemas and linkbases. Schemas are the topic of chapter 4, whereas linkbases are the topic of chapter 5.
TQA offers a **yaidom dialect** for XBRL schema data within one document, and it offers a **TQA query API**
for XBRL schema data across schema documents. Note that most interesting queries on schema data are not
local to individual schema documents. For example, global element declarations may have types or substitution
groups defined in other schema documents.

Before doing the exercises of chapter 4, make sure to have a decent understanding of XML Schema (outside
the context of XBRL). There are many tutorials on XML Schema, but only few of them are clear on XML Schema
and namespaces, on the type system, and on substitution groups. For those new to XML Schema, the article
`XML Schema: An Overview`_ could be a nice start. The author of the article, Priscilla Walmsley, is also
the author of `Definitive XML Schema`_, which in my perception is the best book on XML Schema I have ever
come across. If reading a book like this takes too much time, the articles `Understanding XML Schema`_ and
`W3C XML Schema Design Patterns: Avoiding Complexity`_ (opinionated, but instructive) could be nice follow-up
reads after the above-mentioned article. This should give enough understanding of XML Schema in order to
start understanding XML Schema in an XBRL taxonomy context.

Next, turn to the exercises of chapter 4. Using the TQA "type-safe DOM" and the TQA query API, it is shown
how to relate facts in XBRL instances to (TQA) taxonomy schema content, and how to navigate through
taxonomy schema content. Substitution groups play a very important role, since all facts in an XBRL instance
are declared by global element declarations having substitution group **xbrli:item** or **xbrli:tuple**, or a
derived substitution group.

After doing the exercises of chapter 4, the following should be clear:

* TQA as a "yaidom dialect" and query API for taxonomy schema content is far easier and safer to use than "raw yaidom"
* XML Schema content is to a large extent modeled in TQA
* Yet the focus is on XML Schema content in an XBRL context, and on XBRL substitution groups in particular

.. _`XML Schema: An Overview`: http://www.informit.com/articles/article.aspx?p=25002
.. _`Definitive XML Schema`: http://www.datypic.com/books/defxmlschema/
.. _`Understanding XML Schema`: https://msdn.microsoft.com/en-us/library/aa468557.aspx
.. _`W3C XML Schema Design Patterns: Avoiding Complexity`: https://msdn.microsoft.com/en-us/library/aa468564.aspx


Chapter 5
=========

Chapter 5 introduces **relationships**, from the perspective of TQA. Relationships are at a higher level of abstraction
than XLink arcs; they are like the arcs where the source and target of the arc have been resolved.

In chapter 2 XLink arcs in the TQA DOM were treated, with awareness of XML Base and XPointer (as used in XBRL).
In the exercises of chapter 2 (partial) "resolution" of XLink arc sources and targets was done by hand. In chapter 5
it is shown that TQA can do this resolution itself, lifting the abstraction level from XLink arcs to relationships.
For example, an XLink arc with arcrole ``http://www.xbrl.org/2003/arcrole/parent-child`` and element (expanded) name
``{http://www.xbrl.org/2003/linkbase}presentationArc`` represents a ``ParentChildRelationship`` from one concept (as EName)
to another concept (as EName). One XLink arc may even represent more than one relationship (if the source or target
XLink label is used multiple times within the same extended link), but typically that is not the case.

In typical usage of TQA, querying for relationships is far more common than querying for low-level XLink arcs.
If needed, we can always descend from the relationship to the underlying XLink arc and locators/resources at the TQA
DOM level, but this is rarely needed.

Querying XBRL taxonomies using TQA involves the following "layers":

* The *TQA query API*, in the "queryapi" package. The queries mostly return relationships or taxonomy DOM elements such as concept declarations (see chapter 4).
* The *relationship* type hierarchy in the "relationship" package. This is the most commonly used data in typical TQA usage.
* The type-safe XBRL *taxonomy DOM* type hierarchy in the "dom" package. See chapter 4.

When querying for linkbase content (at a higher level of abstraction) we typically query for relationships, and when
querying for schema content we typically query for TQA DOM elements.

After reading this introduction to chapter 5, turn to the exercises of chapter 5. In these exercises querying for
relationships is practiced, but querying for taxonomy schema content is practiced as well. Indeed, in the chapter 5
exercises the material of the preceding chapters (especially chapter 4) comes back. Chapter 5 is the most important
chapter of the TQA workshop (but it cannot be seen in isolation from the preceding chapters), because *chapter 5 shows
how to use TQA in practice*.

After doing the exercises of chapter 5, the following should be clear:

* How to use TQA for typical XBRL taxonomy querying tasks, searching for relationships and taxonomy schema content.
* How the material of chapters 1, 2, 4 and 5 "hangs together", and how TQA is "layered" and uses XLink and yaidom to provide a higher level of abstraction.
* How TQA and the XBRL instance model of chapter 3 use yaidom for supporting "XML dialects" (and for abstracting over "XML backends").

As mentioned above, in chapter 5 it is learned how to use TQA in practice. This applies to XBRL taxonomies as specified by the Core
XBRL specification. XBRL dimensions are not treated in chapter 5. That is the topic of chapter 6.
