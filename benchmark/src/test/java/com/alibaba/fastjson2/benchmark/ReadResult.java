package com.alibaba.fastjson2.benchmark;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReadResult {
    public static void main(String[] args) throws Exception {
        File outFile = new File("/Users/wenshao/Work/git/fastjson2/docs/benchmark/benchmark_2.0.27_raw.md");
//        File file = new File("/Users/wenshao/Downloads/result_2.0.25.out");

        Map<String, String> files = new LinkedHashMap<>();
        files.put("ecs.g7.xlarge", "/Users/wenshao/Downloads/result_2.0.27_g7.out");
        files.put("ecs.g7a.xlarge", "/Users/wenshao/Downloads/result_2.0.27_g7a.out");
        files.put("ecs.g8m.xlarge", "/Users/wenshao/Downloads/result_2.0.27_g8m.out");
        files.put("OrangePI5", "/Users/wenshao/Downloads/result_2.0.27_orangepi5.out");
        files.put("AppleM1Pro", "/Users/wenshao/Downloads/result_2.0.27_applem1pro.out");
        files.put("MacBookPro2016", "/Users/wenshao/Downloads/result_2.0.27_mac_book_pro_2019_i9.out");

        PrintStream out = new PrintStream(new FileOutputStream(outFile));
        files.forEach((k, v) -> {
            try {
                File file = new File(v);
                DocReader reader = new DocReader(k, file);
                reader.read();

                for (String line : reader.blockLines) {
                    out.println(line);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        out.close();

        // benchmark_2.0.27_raw.md
    }

    static class DocReader
            implements Closeable {
        final String spec;
        BufferedReader reader;
        String vmVersion;
        String vmInvoker;
        boolean block;
        List<String> blockLines = new ArrayList<>();

        public DocReader(String spec, File file) throws IOException {
            this.spec = spec;
            this.reader = new BufferedReader(new FileReader(file));
        }

        public void read() throws IOException {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                if (line.startsWith("# VM version: ")) {
                    vmVersion = line;
                } else if (line.startsWith("# VM invoker: ")) {
                    vmInvoker = line;
                }

                if (block) {
                    if (line.startsWith("#") || line.startsWith("Error: ") || line.startsWith("./run")) {
                        blockLines.add("```");
                        block = false;
                    }
                }

                if (line.startsWith("Benchmark ")) {
                    block = true;
                    String ends = null;
                    String[] strings = new String[] {
                            "/Contents/Home/jre/bin/java",
                            "/Contents/Home/bin/java",
                            "/jre/bin/java",
                            "/bin/java"
                    };

                    for (String string : strings) {
                        if (vmInvoker.endsWith(string)) {
                            ends = string;
                            break;
                        }
                    }
                    if (ends == null) {
                        throw new IOException("not support VMInvoker");
                    }

                    int index = vmInvoker.lastIndexOf('/', vmInvoker.length() - ends.length() - 1);
                    String vm = vmInvoker.substring(index + 1, vmInvoker.length() - ends.length());
                    if (vm.startsWith("graalvm-ce-java1")) {
                        vm = "graalvm-ce-1" + vm.substring("graalvm-ce-java1".length());
                    } else if (vm.startsWith("graalvm-ee-java1")) {
                        vm = "graalvm-ee-1" + vm.substring("graalvm-ee-java1".length());
                    }
                    String title = "# " + spec + "-" + vm;
                    blockLines.add(title);
                    blockLines.add("```java");
                }

                if (block) {
                    blockLines.add(line);
                }
                // Benchmark                                                 Mode  Cnt     Score     Error   Units
            }

            if (block) {
                blockLines.add("```");
                block = false;
            }
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
