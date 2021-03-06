/*
 * Copyright 2017 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
         US Veterans Health Administration, OSHERA, and the Health Services Platform Consortium..
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
 * limitations under the License.
 */
package sh.komet.gui.provider;

import java.util.HashMap;
import java.util.function.Consumer;
import javafx.scene.Scene;
import sh.komet.gui.contract.StatusMessageService;

/**
 *
 * @author kec
 */
public class StatusMessageProvider implements StatusMessageService {
   
   private final HashMap<String, Consumer<String>> consumerMap = new HashMap<>();
   
   @Override
   public void addScene(Scene scene, Consumer<String> messageConsumer) {
      consumerMap.put(scene.getRoot().getId(), messageConsumer);
   }
   
   @Override
   public void removeScene(Scene scene) {
      consumerMap.remove(scene.getRoot().getId());
   }
   
   @Override
   public void reportSceneStatus(Scene scene, String status) {
      if (consumerMap.containsKey(scene.getRoot().getId())) {
         consumerMap.get(scene.getRoot().getId()).accept(status);
      }
   }

   @Override
   public void reportStatus(String status) {
      consumerMap.values().forEach((consumer) -> consumer.accept(status));
   }
   
}
