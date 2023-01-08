Added some useful filters.
Copypaste, modified noise, grid. They work with a coords-transform kit.

Separated from [MI2U](https://github.com/BlackDeluxeCat/MI2-Utilities-Java)

# 内容（v1.1）
## 一套地形筛选器
### 功能地筛
改良噪声，改良矿物噪声
> 噪声图像支持使用修饰地筛做变换
> 
> 可以指定要替换的目标

生成网格
> 支持使用修饰地筛做变换

复制粘贴
> 从地图指定位置复制一块区域，粘贴到指定位置
> 
> 可选的只应用到墙、地板、矿物

### 坐标变换（修饰地筛）
将它们放在其他地筛前，对遇到的第一个功能地筛叠加应用。

坐标平移、伸缩、旋转、极坐标投射、应用区域限制。

<img width="794" alt="屏幕截图 2023-01-08 194202" src="https://user-images.githubusercontent.com/65377021/211194653-649e661b-28cf-4b50-9578-f9a8224ef2d2.png">

### 提醒
原版不能处理这些筛选器，请不要保存到地图属性里！

## 一套辅助线工具
**替换了原版的网格线工具。**

自定义的辅助线
> 基于函数解析式，众多基本函数可用
> 
> 线宽也可以是解析式，从而变线成形状
> 
> 极坐标/直角坐标切换
> 
> 一键填充图像覆盖区域（用1x1笔刷）
> 
> 操作ui在编辑器左上角

<img width="794" alt="屏幕截图 2023-01-08 195214" src="https://user-images.githubusercontent.com/65377021/211194678-a5736fcb-3794-4408-9432-7ba837e280d4.png">

# 开发者提示
不兼容信息：
* 替换了原版的网格线工具 ui.editor.view.image
