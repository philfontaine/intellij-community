/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes.patch;

import com.intellij.openapi.diff.impl.patch.Base85;
import com.intellij.openapi.diff.impl.patch.BinaryFilePatch;
import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Collection;

import static com.intellij.openapi.vcs.changes.patch.BlobIndexUtil.NOT_COMMITTED_HASH;

public class BinaryPatchWriter {

  private final static String GIT_DIFF_HEADER = "diff --git %s %s";
  private final static String FILE_MODE_HEADER = "%s file mode %d";
  private final static String INDEX_SHA1_HEADER = "index %s..%s";
  private final static String GIT_BINARY_HEADER = "GIT binary patch";
  private final static String LITERAL_HEADER = "literal %d";
  private final static int REGULAR_FILE_MODE = 100644;
  private final static int EXECUTABLE_FILE_MODE = 100755;
  @SuppressWarnings("unused")
  private final static int SYMBOLIC_LINK_MODE = 120000; //now we do not support such cases, but need to keep in mind


  public static void writeBinaries(@Nullable String basePath,
                                   @NotNull Collection<FilePatch> patches,
                                   @NotNull Writer writer,
                                   @NotNull final String lineSeparator) throws IOException {
    for (FilePatch patch : patches) {
      if (!(patch instanceof BinaryFilePatch)) continue;
      BinaryFilePatch filePatch = (BinaryFilePatch)patch;
      writer.write(String.format(GIT_DIFF_HEADER, filePatch.getBeforeName(), filePatch.getAfterName()));
      writer.write(lineSeparator);
      File afterFile = new File(basePath, filePatch.getAfterName());
      if (filePatch.isDeletedFile()) {
        writer.write(String.format(FILE_MODE_HEADER, "deleted", REGULAR_FILE_MODE));
        writer.write(lineSeparator);
      }
      else if (filePatch.isNewFile()) {
        writer.write(String.format(FILE_MODE_HEADER, "new", afterFile.canExecute() ? EXECUTABLE_FILE_MODE : REGULAR_FILE_MODE));
        writer.write(lineSeparator);
      }
      byte[] afterContent = filePatch.getAfterContent();
      writer.write(String.format(INDEX_SHA1_HEADER,
                                 filePatch.isNewFile() ? NOT_COMMITTED_HASH : getSha1ForContent(filePatch.getBeforeContent()),
                                 filePatch.isDeletedFile() ? NOT_COMMITTED_HASH : getSha1ForContent(afterContent)));
      writer.write(lineSeparator);
      writer.write(GIT_BINARY_HEADER);
      writer.write(lineSeparator);
      writer.write(String.format(LITERAL_HEADER, afterContent == null ? 0 : afterContent.length));
      writer.write(lineSeparator);
      Base85.encode(afterFile.exists() ? new FileInputStream(afterFile) : new ByteArrayInputStream(ArrayUtil.EMPTY_BYTE_ARRAY),
                    afterFile.length(), writer);
      writer.write(lineSeparator);
    }
  }

  private static String getSha1ForContent(@Nullable byte[] content) {
    return content != null ? BlobIndexUtil.sha1ForBlob(content) : NOT_COMMITTED_HASH;
  }
}
