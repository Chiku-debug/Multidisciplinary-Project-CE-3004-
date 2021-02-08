import threading
import tkinter as tk

ARENA_W = 15
ARENA_H = 20
ARENA = [[0 for x in range(ARENA_W)] for y in range(ARENA_H)]
canvas = 0
boxes = 0
r = tk.Tk()
canvas = tk.Canvas(r, width=ARENA_W*30, height=ARENA_H*30)
canvas.pack()
boxes = [[0 for x in range(ARENA_W)] for y in range(ARENA_H)]
for x in range(ARENA_W) :
    for y in range(ARENA_H) :
        x1 = x*20
        y1 = y*20
        x2 = x1+20
        y2 = y1+20
        boxes[y][x] = canvas.create_rectangle( x1, y1, x2, y2, outline='black', fill="green")
        canvas.update()
def toGuiX(x):
    return ARENA_W - x
def toGuiY(y):
    return ARENA_H - y
def updateBox(x,y):
    color = 'purple'
    if ARENA[y][x] == 0: #Unexplored
        color = 'orange'
    elif ARENA[y][x] == 1: #Explored
        color = 'yellow'
    elif ARENA[y][x] == 2: #Obstacle
        color = 'black'
    x = toGuiX(x)
    y = toGuiY(y)
    #x1 = x*20
    #y1 = y*20
    #x2 = x1+20
    #y2 = y1+20
    #boxes[y][x] = canvas.create_rectangle( x1, y1, x2, y2, outline='black', fill=color)
    boxes[y][x].config(fill=color)
def gui():
    r.mainloop()

def updateGui():
    while True:
        for x in range(ARENA_W) :
            for y in range(ARENA_H) :
                updateBox(x,y)
        r.update()

gui_thread = threading.Thread(target=gui, group=None)
gui_thread.run()

update_gui_thread = threading.Thread(target=updateGui, group=None)
update_gui_thread.run()