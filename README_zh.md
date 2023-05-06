添加实用的地图筛选器，添加强大的图像绘制工具。

功能分离自[MI2U](https://github.com/BlackDeluxeCat/MI2-Utilities-Java)

依赖[MiTour-Lib](https://github.com/BlackDeluxeCat/MiTour-Lib)

- [English](README.md) | [中文](README_zh.md)

# MapFilterExt（v1.6）
## 一套辅助线工具
**替换了原版的网格线工具。**

用数学的函数与方程在坐标系内作图
> 许多实用的基本函数，发挥数学功底和创造力
>
> 极坐标系/直角坐标系
>
> 一键使用画笔涂抹图像
> 
> 导入与导出图像蓝图

<img width="793" alt="屏幕截图 2023-01-08 200247" src="https://user-images.githubusercontent.com/65377021/211195002-aaf909c5-79fb-4218-99ba-9a3aaa0dbb5a.png">

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

### 修饰地筛
修饰地筛堆叠放在其他地筛前，对遇到的第一个功能地筛叠加应用。

坐标平移、伸缩、旋转、极坐标投射、应用区域限制。

<img width="794" alt="屏幕截图 2023-01-08 194202" src="https://user-images.githubusercontent.com/65377021/211194653-649e661b-28cf-4b50-9578-f9a8224ef2d2.png">

### 提醒
原版不能处理Mod筛选器，请不要保存到地图属性里！

# 开发者提示
* 替换的字段：ui.editor.view.image
