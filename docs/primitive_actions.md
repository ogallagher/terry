# Primitive Actions

This lists all primitive actions included in Terry's initial corpus. `action.addState(State(name,value,args,transition))` defines what state transitions and associated driver executions occur when this action is called.

<hr>

## mouseToXY

### Description

Moves the mouse to screen coordinates (`x`,`y`), where both are integer numbers. Later on I want to modify this to also have a time argument for controlling easing speed.

### Pattern

`?move) |mouse,cursor,pointer,) to ?|location,position,coordinates,)) ?ex) @#x |ex,comma,why) @#y ?why)`

### States

```java
new State<Point2D>("mouseat", new Point2D.Float(0,0), new String[] {"x","y"}, new DriverExecution<Point2D>() {
    public Point2D execute(Point2D stateOld, Arg[] args) {
        Float x = (Float) args[0].value;
        Float y = (Float) args[1].value;
        Driver.point(x,y);
        return new Point2D.Float(x,y);
    }
});
```

<hr>

## typeStr

### Description

Controls the keyboard to type a string `str`. Later on I want this to have a time argument for controlling typing speed.

### Pattern

`type ?out) ?following string) @$str ?end quote)`

### States

```java
new State<String>("typed", "", new String[] {"str"}, new DriverExecution<String>() {
    public String execute(String stateOld, Arg[] args) {
        String string = (String) args[0].value;
        Driver.type(string);
        return string;
    }
});
```

<hr>

## mouseClickBtn

### Description

Clicks a mouse button (left or right) specified by the `btn` argument.

### Pattern

`?|left,right,)) click`

### States

```java
new State<Integer>("clickbtn", new Integer(), new String[] {"btn"}, new DriverExecution<Integer>() {
    public Integer execute(Integer stateOld, Arg[] args) {
        Integer button = (Integer) args[0].value;
        if (button == MouseEvent.BUTTON1) {
            Driver.clickLeft();
        }
        else if (button == MouseEvent.BUTTON2) {
            Driver.clickRight();
        }
        return button;
    }
});
```