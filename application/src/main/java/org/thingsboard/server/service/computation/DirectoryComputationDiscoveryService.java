/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.service.computation;

import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.file.DirectoryChange;
import akka.stream.alpakka.file.javadsl.Directory;
import akka.stream.alpakka.file.javadsl.DirectoryChangesSource;
import akka.stream.javadsl.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.server.common.msg.computation.ComputationScanFinished;
import org.thingsboard.server.common.msg.computation.SparkComputationAdded;
import org.thingsboard.server.utils.MiscUtils;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DirectoryComputationDiscoveryService implements ComputationDiscoveryService{

    @Value("${spark.jar_path}")
    private String libraryPath;

    @Value("${spark.polling_interval}")
    private Long pollingInterval;

    private ComputationDiscoveryListener listener;
    private ActorMaterializer materializer;

    @Override
    public void init(ComputationDiscoveryListener listener, ActorMaterializer materializer) {
        Assert.hasLength(libraryPath, MiscUtils.missingProperty("spark.jar_path"));
        Assert.notNull(pollingInterval, MiscUtils.missingProperty("spark.polling_interval"));
        this.listener = listener;
        this.materializer = materializer;
        processExistingComputations();
    }

    private void processExistingComputations(){
        final FileSystem fs = FileSystems.getDefault();
        //final Source<Path, NotUsed> source = Directory.walk(fs.getPath(libraryPath));
        try {
            List<Path> jars = Files.walk(fs.getPath(libraryPath)).collect(Collectors.toList());
            for(Path j: jars){
                if(isJar(j)) {
                    listener.onMsg(new SparkComputationAdded(j));
                }
            }
            listener.onMsg(new ComputationScanFinished());
            startPolling();
        } catch (IOException e) {
            log.error("Error while reading jars", e);
        }
        /*source.runForeach((Path p) -> {
            if(isJar(p)) {
                listener.onMsg(new SparkComputationAdded(p));
            }
        }, materializer).whenComplete((d, e) -> {
            if(d != null){
                listener.onMsg(new ComputationScanFinished());
                startPolling();
            }else{
                log.error("Error occurred while reading jars from directory", e);
            }
        });*/
    }

    private void startPolling(){
        final FileSystem fs = FileSystems.getDefault();
        final FiniteDuration interval = FiniteDuration.create(pollingInterval, TimeUnit.SECONDS);
        final int maxBufferSize = 1000;
        final Source<Pair<Path, DirectoryChange>, NotUsed> changes =
                DirectoryChangesSource.create(fs.getPath(libraryPath), interval, maxBufferSize);


        changes.runForeach((Pair<Path, DirectoryChange> pair) -> {
            final Path changedPath = pair.first();
            final DirectoryChange change = pair.second();
            if(isJar(changedPath) && change == DirectoryChange.Creation){
                listener.onMsg(new SparkComputationAdded(changedPath));
            }
        }, materializer);
    }

    private boolean isJar(Path jarPath) throws IOException {
        File file = jarPath.toFile();
        return file.getCanonicalPath().endsWith(".jar") && file.canRead();
    }
}
