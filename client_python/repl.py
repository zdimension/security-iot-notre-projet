from card import Card, init_card
import traceback

init_card()
Card.hello()

commands = {name: func
            for name, func
            in Card.__dict__.items() if callable(func) and not name.startswith("_")}

while True:
    command = input("> ").strip()
    if not command:
        continue
    cmd, *args = command.split()
    if cmd == "exit":
        break
    elif cmd == "help":
        print("Commands: " + ", ".join(commands))
    elif fct := commands.get(cmd):
        try:
            fct(*args)
        except Exception as e:
            print(e)
            traceback.print_exc()
    else:
        print("Unknown command")
