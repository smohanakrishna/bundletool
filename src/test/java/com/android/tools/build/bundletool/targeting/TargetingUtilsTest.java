/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tools.build.bundletool.targeting;

import static com.android.tools.build.bundletool.targeting.TargetingUtils.getMaxSdk;
import static com.android.tools.build.bundletool.targeting.TargetingUtils.getMinSdk;
import static com.android.tools.build.bundletool.testing.TargetingUtils.abiTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.assetsDirectoryTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.graphicsApiTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.languageTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.mergeAssetsTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.openGlVersionFrom;
import static com.android.tools.build.bundletool.testing.TargetingUtils.sdkVersionFrom;
import static com.android.tools.build.bundletool.testing.TargetingUtils.sdkVersionTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.textureCompressionTargeting;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

import com.android.bundle.Targeting.Abi.AbiAlias;
import com.android.bundle.Targeting.AssetsDirectoryTargeting;
import com.android.bundle.Targeting.SdkVersion;
import com.android.bundle.Targeting.SdkVersionTargeting;
import com.android.bundle.Targeting.TextureCompressionFormat.TextureCompressionFormatAlias;
import com.android.bundle.Targeting.VariantTargeting;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TargetingUtilsTest {

  @Test
  public void getDimensions_noDimension() {
    assertThat(TargetingUtils.getTargetingDimensions(AssetsDirectoryTargeting.getDefaultInstance()))
        .isEmpty();
  }

  @Test
  public void getDimensions_graphicsApi() {
    assertThat(
            TargetingUtils.getTargetingDimensions(
                assetsDirectoryTargeting(graphicsApiTargeting(openGlVersionFrom(2, 3)))))
        .containsExactly(TargetingDimension.GRAPHICS_API);
  }

  @Test
  public void getDimensions_language() {
    assertThat(
            TargetingUtils.getTargetingDimensions(
                assetsDirectoryTargeting(languageTargeting("en"))))
        .containsExactly(TargetingDimension.LANGUAGE);
  }

  @Test
  public void getDimensions_textureCompressionFormat() {
    assertThat(
            TargetingUtils.getTargetingDimensions(
                assetsDirectoryTargeting(
                    textureCompressionTargeting(TextureCompressionFormatAlias.ATC))))
        .containsExactly(TargetingDimension.TEXTURE_COMPRESSION_FORMAT);
  }

  @Test
  public void getDimensions_abi() {
    assertThat(
            TargetingUtils.getTargetingDimensions(
                assetsDirectoryTargeting(abiTargeting(AbiAlias.ARM64_V8A))))
        .containsExactly(TargetingDimension.ABI);
  }

  @Test
  public void getDimensions_multiple() {
    assertThat(
            TargetingUtils.getTargetingDimensions(
                mergeAssetsTargeting(
                    assetsDirectoryTargeting(
                        textureCompressionTargeting(TextureCompressionFormatAlias.ATC)),
                    assetsDirectoryTargeting(
                        abiTargeting(
                            ImmutableSet.of(AbiAlias.ARM64_V8A, AbiAlias.ARMEABI_V7A),
                            ImmutableSet.of())),
                    assetsDirectoryTargeting(graphicsApiTargeting(openGlVersionFrom(3))))))
        .containsExactly(
            TargetingDimension.ABI,
            TargetingDimension.TEXTURE_COMPRESSION_FORMAT,
            TargetingDimension.GRAPHICS_API);
  }

  @Test
  public void generateAllVariantTargetings_singleVariant() {
    assertThat(
            TargetingUtils.generateAllVariantTargetings(
                ImmutableSet.of(variantTargetingFromSdkVersion(sdkVersionFrom(1)))))
        .containsExactly(variantTargetingFromSdkVersion(sdkVersionFrom(1)));
  }

  @Test
  public void generateAllVariantTargetings_disjointVariants() {
    assertThat(
            TargetingUtils.generateAllVariantTargetings(
                ImmutableSet.of(
                    variantTargetingFromSdkVersion(
                        sdkVersionFrom(21), ImmutableSet.of(sdkVersionFrom(23))),
                    variantTargetingFromSdkVersion(
                        sdkVersionFrom(23), ImmutableSet.of(sdkVersionFrom(21))))))
        .containsExactly(
            variantTargetingFromSdkVersion(sdkVersionFrom(21), ImmutableSet.of(sdkVersionFrom(23))),
            variantTargetingFromSdkVersion(
                sdkVersionFrom(23), ImmutableSet.of(sdkVersionFrom(21))));
  }

  @Test
  public void generateAllVariantTargetings_overlappingVariants() {
    assertThat(
            TargetingUtils.generateAllVariantTargetings(
                ImmutableSet.of(
                    variantTargetingFromSdkVersion(
                        sdkVersionFrom(21), ImmutableSet.of(sdkVersionFrom(25))),
                    variantTargetingFromSdkVersion(
                        sdkVersionFrom(25), ImmutableSet.of(sdkVersionFrom(21))),
                    variantTargetingFromSdkVersion(
                        sdkVersionFrom(21), ImmutableSet.of(sdkVersionFrom(23))),
                    variantTargetingFromSdkVersion(
                        sdkVersionFrom(23), ImmutableSet.of(sdkVersionFrom(21))))))
        .containsExactly(
            variantTargetingFromSdkVersion(
                sdkVersionFrom(21), ImmutableSet.of(sdkVersionFrom(23), sdkVersionFrom(25))),
            variantTargetingFromSdkVersion(
                sdkVersionFrom(23), ImmutableSet.of(sdkVersionFrom(21), sdkVersionFrom(25))),
            variantTargetingFromSdkVersion(
                sdkVersionFrom(25), ImmutableSet.of(sdkVersionFrom(21), sdkVersionFrom(23))));
  }

  @Test
  public void minSdk_emptySdkTargeting() {
    assertThat(getMinSdk(SdkVersionTargeting.getDefaultInstance())).isEqualTo(1);
  }

  @Test
  public void minSdk_nonEmptySdkTargeting() {
    assertThat(
            getMinSdk(sdkVersionTargeting(sdkVersionFrom(21), ImmutableSet.of(sdkVersionFrom(23)))))
        .isEqualTo(21);
  }

  @Test
  public void maxSdk_emptySdkTargeting() {
    assertThat(getMaxSdk(SdkVersionTargeting.getDefaultInstance())).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void maxSdk_nonEmptySdkTargeting() {
    assertThat(
            getMaxSdk(sdkVersionTargeting(sdkVersionFrom(21), ImmutableSet.of(sdkVersionFrom(23)))))
        .isEqualTo(23);
    assertThat(
            getMaxSdk(sdkVersionTargeting(sdkVersionFrom(23), ImmutableSet.of(sdkVersionFrom(21)))))
        .isEqualTo(Integer.MAX_VALUE);
  }

  private static VariantTargeting variantTargetingFromSdkVersion(SdkVersion values) {
    return variantTargetingFromSdkVersion(values, ImmutableSet.of());
  }

  private static VariantTargeting variantTargetingFromSdkVersion(
      SdkVersion values, ImmutableSet<SdkVersion> alternatives) {
    return VariantTargeting.newBuilder()
        .setSdkVersionTargeting(sdkVersionTargeting(values, alternatives))
        .build();
  }
}
