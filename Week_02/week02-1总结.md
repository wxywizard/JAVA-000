> cms一般不和并行gc搭配使用
>
> young gc 一般15次之后会放到old区，默认可能是7次左右放到old区 一般to区放不下也会放到old区
>
> spring ioc的Bean默认情况下是单例的 静态的
>
> meta区放的类的定义，结构，元数据  class对象它的基础的信息