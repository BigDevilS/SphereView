# SphereView
模拟球面的ViewGroup

添加依赖：
```gradle
allprojects {
    repositories {
        ......
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    ......
    implementation 'com.github.BigDevilS:SphereView:v1.0.3'
}
```

## Attrs
Name|Format|Default|Description
--|--|--|--
min_scale|float|0.3f|最小缩放比例
max_scale|float|1f|最大缩放比例
min_alpha|float|0.3f|最低透明度比例
max_elevation|dimension|10dp|z轴最大高度
loop_speed|dimension|2dp|自动旋转速度
loop_angle|int|45|自动旋转角度
## APIs

Method|Description
--|--
startLoop()|开始自动旋转
stopLoop()|停止自动旋转
setMinScale(float)|设置最小缩放比例
setMaxScale(float)|设置最大缩放比例
setMinAlpha(float)|设置最低透明度比例
setMaxElevation(float)|设置z轴最大高度
setLoopSpeed(int)|设置自动旋转速度
setLoopAngle(int)|设置自动旋转角度

## Demo
[demo.apk](https://github.com/BigDevilS/SphereView/raw/master/previews/demo.apk)

## Previews
![image](https://github.com/BigDevilS/SphereView/blob/master/previews/1.gif)
![image](https://github.com/BigDevilS/SphereView/blob/master/previews/2.gif)
![image](https://github.com/BigDevilS/SphereView/blob/master/previews/3.gif)
