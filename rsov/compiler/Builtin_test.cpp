/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "Builtin.h"

#include "file_utils.h"
#include "spirit.h"
#include "test_utils.h"
#include "gtest/gtest.h"

namespace android {
namespace spirit {

TEST(BuiltinTest, testBuiltinTranslation) {
  const std::string testFile("greyscale.spv");
  const std::string testDataPath(
      "frameworks/rs/rsov/compiler/spirit/test_data/");
  const std::string &fullPath = getAbsolutePath(testDataPath + testFile);
  auto words = readFile<uint32_t>(fullPath);
  std::unique_ptr<InputWordStream> IS(
      InputWordStream::Create(std::move(words)));
  std::unique_ptr<Module> m(Deserialize<Module>(*IS));

  ASSERT_NE(nullptr, m);

  Builder b;
  m->setBuilder(&b);

  int error;
  auto words1 = rs2spirv::TranslateBuiltins(b, m.get(), &error);

  ASSERT_EQ(0, error);

  std::unique_ptr<InputWordStream> IS1(
      InputWordStream::Create(std::move(words1)));
  std::unique_ptr<Module> m1(Deserialize<Module>(*IS1));

  ASSERT_NE(nullptr, m1);
}

} // spirit
} // android
