/*
 * Copyright 2026 Pete Bentley <pete@sorted.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.prbentley.scratchpad.sketch;

import org.jetbrains.annotations.NotNull;

import java.security.Security;
import java.util.List;

import org.prbentley.scratchpad.BaseSketch;
import org.prbentley.scratchpad.BooleanWidget;
import org.prbentley.scratchpad.Default;
import org.prbentley.scratchpad.Sketch;
import org.prbentley.scratchpad.Widget;

@Sketch
@Default
public class HelloJava extends BaseSketch {
  List<String> properties = List.of(
          "java.version",
          "java.home",
          "os.name",
          "os.arch",
          "os.version",
          "user.home",
          "user.dir"
          );

  private final BooleanWidget showProperties =
      new BooleanWidget("Would you like to see some Java and Security properties?", true);

  @Override
  public @NotNull List<@NotNull Widget> getWidgets() {
    return List.of(showProperties);
  }

  @Override
  public void go() {
    out("Hello World in Java.");

    if (showProperties.getChecked()) {
      out("\nProperties:");
      for (String property : properties) {
        out("%s: %s", property, System.getProperty(property));
      }

      out("\nSecurity Properties for Providers");
      for (int i = 1; i < 10; i++) {
        String key = "security.provider." + i;
        String value = Security.getProperty(key);
        if (value != null) {
          out("%s: %s", key, value);
        }
      }
    }
  }
}
