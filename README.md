# update

## 0.1.1
- 增加redis对象缓存模板

## 0.1.3
- 新增规格换算工具`PackingConvert`

## 0.1.4
- 移除工具`MapUl`，`Map`有自带`#computeIfAbsent(...)`
- 移除工具`StringFormatUtils#valueOf(...)`，有`org.apache.commons.lang3.ObjectUtils#toString(Object, Supplier)`
- 移除工具`PackingConvert`中对单位"瓶"和"盒"的同义字典，因为"瓶"发现了和"支"的同义场景（Δ 10ml*18支/盒、10ml*18瓶），而"支"和"盒"不同义，且发现"3瓶/盒"
- 移除工具`HttpRequestBuilder`中对`IOException`的捕获封装。改为声明抛出，用户自行捕获

## 0.1.5
- 工具`HttpRequestBuilder`增加cookie支持，请求完成后通过创建的实例`builder#getCookies()`获取
- 修复`PackingConvert`内部缓存问题，混淆相同规格串、但单位不同的转换图
- 工具`PackingConvert`同义词新增"枚"，与"粒"、"片"、"丸"等同义

## 0.1.6
- 工具`PackingConvert`，新增一些默认的单位的自动转换，[ml,l]、[mg,g,kg]
- 工具`HttpRequestBuilder`增加proxy支持

## 0.1.7
- 修复`SynchronizedUtils`多重锁时释放问题

# feature 