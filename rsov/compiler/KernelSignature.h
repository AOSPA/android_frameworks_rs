/*
 * Copyright 2016, The Android Open Source Project
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

#ifndef RS_KERNEL_SIGNATURE_H
#define RS_KERNEL_SIGNATURE_H

#include "llvm/ADT/StringRef.h"
#include "llvm/IR/DerivedTypes.h"

#include <string>
#include <vector>

namespace rs2spirv {

static const llvm::StringRef CoordsNames[] = {"x", "y", "z"};

// Numeric value corresponds to the number of components.
enum class Coords : size_t { None = 0, X, XY, XYZ, Last = XYZ };

struct KernelSignature {
  typedef std::vector<std::string> ArgumentTypes;
  std::string returnType;
  std::string name;
  ArgumentTypes argumentTypes;
  Coords coordsKind;

  KernelSignature(const llvm::FunctionType *FT, const std::string Fname,
                  Coords CK);

  void dump() const;

  inline std::string getWrapperName(void) const {
    return wrapperPrefix + "entry_" + name;
  }

  inline std::string getTempName(const std::string suffix) const {
    return wrapperPrefix + name + "_" + suffix;
  }

  static bool isWrapper(const llvm::StringRef &id) {
    return id.startswith(wrapperPrefix);
  }

private:
  static const std::string wrapperPrefix;
};

} // namespace rs2spirv

#endif
