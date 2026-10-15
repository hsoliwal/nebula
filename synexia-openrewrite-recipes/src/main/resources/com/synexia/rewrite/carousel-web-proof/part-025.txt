<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Third-party notices for the Carousel GWT browser profile

The licenses below apply to their respective files and components. This notice does not relicense Eclipse-derived source as Apache software.

## Eclipse Nebula Carousel derivatives

Carousel.java and ImageContainer.java in src/main/java/org/eclipse/nebula/widgets/carousel/client are browser-profile adaptations of the existing Eclipse Nebula Carousel and ImageContainer owners. CarouselGeometry.java and the original geometry reference preserve the original image-fitter derivation and their per-file license headers.

Original author: Copyright (c) 2020 Laurent CARON. Contributors: Laurent CARON (laurent.caron at gmail dot com), initial API and implementation. The browser adaptations identify Hitesh Soliwal and contributors as modification authors. The original copyright, contributor and EPL-2.0 notices remain in the derivative source.

Pinned receiver source: hsoliwal/nebula, commit 648ba48c5827425e3aae58fbd185ac87e518bc49, widgets/carousel/org.eclipse.nebula.widgets.carousel/src/org/eclipse/nebula/widgets/carousel/. Original Carousel blob b56b7a98b030782914a5a00086c8bb8ce57d6416; qualified ImageContainer blob 48ee2e193ad5be19295368932d9f8bc42f4ee99a.

Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/ . The original SWT sources are unchanged by this browser source profile. New example, test, module, documentation and self-created SVG infrastructure with Apache-2.0 headers remain separate from the EPL derivatives.

## GWT 2.12.2 browser runtime

This profile uses GWT Canvas, Context2d, Timer, DOM, event and widget APIs. Actual GWT compilation can include GWT client/runtime implementation in the emitted JavaScript. GWT includes work by Google Inc., the GWT authors and contributors; their selected source notices remain applicable. The included Apache License 2.0 is copied byte-for-byte from the provisioned gwt-user-2.12.2.jar, com/google/gwt/core/COPYING. The same bytes appear in that JAR's emul, i18n, json, user and xml COPYING members.

SDK JAR SHA256: 2a853239980f298bd50ac7dd798bdf4fae9256e0c61415db88a6ee4a92ed3d16. License member SHA256: a6cba85bc92e0cff7a450b1d873c0eaa2e9fc96bf472df0247a26bec77bf3ff9. The full license terms are in licenses/APACHE-2.0.txt and must accompany any distributed browser artifact containing GWT runtime code.

GWT project: https://www.gwtproject.org/ . Source: https://github.com/gwtproject/gwt . Inspect the exact emitted artifact before distribution for additional applicable runtime notices; this packet does not redistribute the full GWT SDK/compiler or claim all compiler dependencies are included in the browser runtime.

## Reuse and execution boundary

FOSS_REUSE_DECISION.tsv preserves the physically prior phase donor decisions. PROVENANCE.tsv records selected component identities and notice sources. ALGORITHM_EVALUATIONS.tsv separately records the required problem-category references; no LeetCode, HackerRank or GeeksForGeeks solution implementation or third-party image artwork was copied. The two example SVG images were created for this packet.

These are recipe authoring inputs. Their presence does not establish recipe application, compilation, numeric/browser behavior, native SWT/RAP compatibility, performance, review approval or completion of the full Java/JNI/UI goal.
