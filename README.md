# RealtimeBlurView

Android实时高斯模糊的View。从 [mmin18/RealtimeBlurView](https://github.com/mmin18/RealtimeBlurView) 改版而来

具体说明参考原库.

本库的改动为：
- 使用 Google 推荐的 [RenderScript-toolkit](https://github.com/android/renderscript-intrinsics-replacement-toolkit) 用于高斯模糊。（支持16K page size）
- 使用 kotlin 重构

### 使用

#### 导入依赖
```gradle
implementation("io.github.limuyang2:realtimeblurview:1.0.3")

// 导入RenderScript-toolkit。 也可以使用任何你编译的RenderScript-toolkit的库
implementation("io.github.limuyang2:renderscrip-toolkit:1.0.1") 
```

#### 使用
```xml
<io.github.limuyang2.realtimeblur.RealtimeBlurView 
    android:id="@+id/blur_view"
    android:layout_height="match_parent" 
    android:layout_width="match_parent"
    app:realtimeBlurRadius="20" />
```

### Thanks
- [mmin18/RealtimeBlurView](https://github.com/mmin18/RealtimeBlurView)
