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

package com.android.tools.build.bundletool.splitters;

import static com.android.tools.build.bundletool.model.ManifestMutator.withSplitsRequired;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.android.bundle.Files.TargetedNativeDirectory;
import com.android.bundle.Targeting.Abi;
import com.android.bundle.Targeting.Abi.AbiAlias;
import com.android.bundle.Targeting.AbiTargeting;
import com.android.bundle.Targeting.NativeDirectoryTargeting;
import com.android.tools.build.bundletool.exceptions.CommandExecutionException;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.build.bundletool.model.ModuleSplit;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;

/** Splits the native libraries in the module by ABI. */
public class AbiNativeLibrariesSplitter implements ModuleSplitSplitter {

  private final boolean include64BitLibs;

  public AbiNativeLibrariesSplitter(boolean include64BitLibs) {
    this.include64BitLibs = include64BitLibs;
  }

  public AbiNativeLibrariesSplitter() {
    this(/* include64BitLibs= */ true);
  }

  /** Generates {@link ModuleSplit} objects dividing the native libraries by ABI. */
  @Override
  public ImmutableCollection<ModuleSplit> split(ModuleSplit moduleSplit) {
    if (!moduleSplit.getNativeConfig().isPresent()) {
      return ImmutableList.of(moduleSplit);
    }

    ImmutableList.Builder<ModuleSplit> splits = new ImmutableList.Builder<>();
    // Flatten all targeted directories.
    List<TargetedNativeDirectory> allTargetedDirectories =
        moduleSplit.getNativeConfig().get().getDirectoryList();
    // Currently we only support targeting via ABI, so grouping it by Targeting.equals() should be
    // enough.
    ImmutableMultimap<NativeDirectoryTargeting, TargetedNativeDirectory> targetingMap =
        Multimaps.index(allTargetedDirectories, TargetedNativeDirectory::getTargeting);
    ImmutableSet<Abi> allAbis =
        targetingMap
            .keySet()
            .stream()
            .map(NativeDirectoryTargeting::getAbi)
            .collect(toImmutableSet());

    // We need to know the exact set of ABIs that we will generate, to set alternatives correctly.
    ImmutableSet<Abi> abisToGenerate =
        allAbis.stream().filter(abi -> include64BitLibs || !is64Bit(abi)).collect(toImmutableSet());

    if (abisToGenerate.isEmpty() && !include64BitLibs) {
      throw CommandExecutionException.builder()
          .withMessage(
              "Generation of 64-bit native libraries is disabled, but App Bundle contains "
                  + "only 64-bit native libraries.")
          .build();
    }

    // Any entries not claimed by the ABI splits will be returned in a separate split using the
    // original targeting.
    HashSet<ModuleEntry> leftOverEntries = new HashSet<>(moduleSplit.getEntries());
    for (NativeDirectoryTargeting targeting : targetingMap.keySet()) {
      ImmutableList<ModuleEntry> entriesList =
          targetingMap
              .get(targeting)
              .stream()
              .flatMap(directory -> moduleSplit.findEntriesUnderPath(directory.getPath()))
              .collect(toImmutableList());

      if (!is64Bit(targeting.getAbi()) || include64BitLibs) {
        ModuleSplit.Builder splitBuilder =
            moduleSplit
                .toBuilder()
                .setApkTargeting(
                    moduleSplit
                        .getApkTargeting()
                        .toBuilder()
                        .setAbiTargeting(
                            AbiTargeting.newBuilder()
                                .addValue(targeting.getAbi())
                                .addAllAlternatives(
                                    Sets.difference(
                                        abisToGenerate, ImmutableSet.of(targeting.getAbi()))))
                        .build())
                .setMasterSplit(false)
                .addMasterManifestMutator(withSplitsRequired(true))
                .setEntries(entriesList);
        splits.add(splitBuilder.build());
      }
      leftOverEntries.removeAll(entriesList);
    }
    if (!leftOverEntries.isEmpty()) {
      splits.add(moduleSplit.toBuilder().setEntries(ImmutableList.copyOf(leftOverEntries)).build());
    }
    return splits.build();
  }

  private static boolean is64Bit(Abi abi) {
    return abi.getAlias().equals(AbiAlias.ARM64_V8A)
        || abi.getAlias().equals(AbiAlias.X86_64)
        || abi.getAlias().equals(AbiAlias.MIPS64);
  }
}
