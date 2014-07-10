/*
 * Copyright 2009-2013 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.asterix.external.indexing.input;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;

import edu.uci.ics.asterix.metadata.entities.ExternalFile;
import edu.uci.ics.hyracks.algebricks.common.exceptions.NotImplementedException;

//Used in two cases:
//1. building an index over a dataset
//2. performing full scan over a dataset that has built index (to provide consistent view) with RCFile format

@SuppressWarnings({ "rawtypes", "deprecation" })
public class RCFileDataReader extends AbstractHDFSReader {

    private RecordReader reader;
    private Object key;
    private Object value;
    private int currentSplitIndex = 0;
    private String fileName;
    private long recordGroupOffset;
    private long nextRecordGroupOffset;
    private boolean executed[];
    private InputSplit[] inputSplits;
    private String[] readSchedule;
    private String nodeName;
    private JobConf conf;
    private List<ExternalFile> files;
    private FileSystem hadoopFS;

    public RCFileDataReader(InputSplit[] inputSplits, String[] readSchedule, String nodeName, JobConf conf,
            boolean executed[], List<ExternalFile> files) throws IOException {
        this.executed = executed;
        this.inputSplits = inputSplits;
        this.readSchedule = readSchedule;
        this.nodeName = nodeName;
        this.conf = conf;
        this.files = files;
        hadoopFS = FileSystem.get(conf);
    }

    private boolean moveToNext() throws IOException {
        for (; currentSplitIndex < inputSplits.length; currentSplitIndex++) {
            /**
             * read all the partitions scheduled to the current node
             */
            if (readSchedule[currentSplitIndex].equals(nodeName)) {
                /**
                 * pick an unread split to read synchronize among
                 * simultaneous partitions in the same machine
                 */
                synchronized (executed) {
                    if (executed[currentSplitIndex] == false) {
                        executed[currentSplitIndex] = true;
                    } else {
                        continue;
                    }
                }

                /**
                 * read the split
                 */
                try {
                    if (files != null) {
                        fileName = ((FileSplit) (inputSplits[currentSplitIndex])).getPath().toUri().getPath();
                        FileStatus fileStatus = hadoopFS.getFileStatus(new Path(fileName));
                        //skip if not the same file stored in the files snapshot
                        if (fileStatus.getModificationTime() != files.get(currentSplitIndex).getLastModefiedTime()
                                .getTime())
                            continue;
                    }
                    reader = getRecordReader(currentSplitIndex);
                    recordGroupOffset = -1;
                    nextRecordGroupOffset = reader.getPos();
                } catch (Exception e) {
                    continue;
                }
                key = reader.createKey();
                value = reader.createValue();
                return true;
            }
        }
        return false;
    }

    @Override
    public int read(byte[] buffer, int offset, int len) throws IOException {
        throw new NotImplementedException("Use readNext()");
    }

    @Override
    public int read() throws IOException {
        throw new NotImplementedException("Use readNext()");
    }

    private RecordReader getRecordReader(int slitIndex) throws IOException {
        RecordReader reader;
        try{
        reader = conf.getInputFormat().getRecordReader(
                (org.apache.hadoop.mapred.FileSplit) inputSplits[slitIndex], conf, getReporter());
        } catch(Exception e){
            e.printStackTrace();
            throw e;
        }
        return reader;
    }

    @Override
    public boolean initialize() throws IOException {
        return moveToNext();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object readNext() throws IOException {

        if (reader == null) {
            return null;
        }
        if (reader.next(key, value)) {
            if (reader.getPos() != nextRecordGroupOffset) {
                recordGroupOffset = nextRecordGroupOffset;
                nextRecordGroupOffset = reader.getPos();
            }
            return value;
        }
        while (moveToNext()) {
            if (reader.next(key, value)) {
                if (reader.getPos() != nextRecordGroupOffset) {
                    recordGroupOffset = nextRecordGroupOffset;
                    nextRecordGroupOffset = reader.getPos();
                }
                return value;
            }
        }
        return null;
    }

    @Override
    public String getFileName() throws Exception {
        return files.get(currentSplitIndex).getFileName();
    }

    @Override
    public long getReaderPosition() throws Exception {
        return recordGroupOffset;
    }

    @Override
    public int getFileNumber() throws Exception {
        return files.get(currentSplitIndex).getFileNumber();
    }

}