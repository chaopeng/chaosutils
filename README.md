# ChaosUtils - 巢鹏的Java工具类 #

ChaosUtils目前包含:

* Zip Deflater压缩/解压
* BaseX [2-62]进制与10进制的转换
* ClassPathScanner 包扫描器
* DirUtils 文件目录工具
* ScriptsUtils java中执行js脚本工具
* UUIDUtils uuid工具，含uuid压缩
* CloseUtils 资源关闭工具，方便关闭可以关闭的资源

ChaosUtils将不会包含:

* DateUtils 日期工具建议使用 [joda-time](http://www.joda.org/joda-time/) ,**请不要考虑使用Java原生日期工具，SimpleDateFormat是非线程安全的，[详细](http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html)**
* 容器简易操作 java7已有钻石语法，java8将支持lambda，[guava](https://code.google.com/p/guava-libraries/)也有相应的实现。
* StringUtils 字符串工具曾经出现果，但是有了guava，不再需要了。
