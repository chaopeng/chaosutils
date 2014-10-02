# ChaosUtils - 巢鹏的Java工具类 #

ChaosUtils目前包含:

* BaseX [2-62]进制与10进制的转换
* ClassPathScanner 包扫描器
* CloseUtils 资源关闭工具，方便关闭可以关闭的资源
* DirUtils 文件目录工具
* OrderedThreadPoolExecutor 轻量级Actor模型的线程池
* ScriptsUtils java中执行js脚本工具
* SortedSet Redis的SortedSet的JAVA实现
* StringUtils 字符串工具，字符串拼接与大小写转换
* UUIDUtils uuid工具，含uuid压缩，含山寨版Twitter的snowflake
* Zip Deflater压缩/解压

ChaosUtils将不会包含:

* DateUtils 日期工具建议使用 [joda-time](http://www.joda.org/joda-time/) ,**请不要考虑使用Java原生日期工具，SimpleDateFormat是非线程安全的，[详细](http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html)**
* 容器简易操作 java7已有钻石语法，java8将支持lambda，[guava](https://code.google.com/p/guava-libraries/)也有相应的实现。
