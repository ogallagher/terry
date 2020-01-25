# Primitive Actions

This lists all primitive actions included in Terry's initial corpus. `action.addState(State(name,value,argNum,transition))` defines what state transitions and associated driver executions occur when this action is called.

<hr>

## mouseToXY

### Description

Moves the mouse to screen coordinates (`x`,`y`), where both are integer numbers. Later on I want to modify this to also have a time argument for controlling easing speed.

### Pattern

`?move) |mouse,cursor,pointer,) to ?|location,position,coordinates,)) ?ex) @#x |ex,comma,why) @#y ?why)`

### States

```java
new State<Point2D>("mouseat", new Point2D.Float(0,0), 2, new DriverExecution<Point2D>() {
    public Point2D execute(Point2D stateOld, Arg[] args) {
        Float x = (Float) args[0];
        Float y = (Float) args[1];
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
new State<String>("typed", "", 1, new DriverExecution<String>() {
    public String execute(String stateOld, Object[] args) {
        String string = (String) args[0];
        Driver.type(string);
        return string;
    }
});
```