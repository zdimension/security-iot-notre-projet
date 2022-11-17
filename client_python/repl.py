from card import Card, init_card
import traceback

init_card()
Card.hello()

commands = Card.commands()

help_msg = "Type 'help' for a list of available commands"
print("Card REPL active.", help_msg)
while True:
    command = input("> ").strip()
    if not command:
        continue
    cmd, *args = command.split()
    if cmd == "exit":
        break
    elif cmd == "help":
        print("Commands:")
        for name, func in commands.items():
            # print command name, right padded, then the func's doc
            # whole line should be indented
            print("  ", name.ljust(20), func.__doc__)
        print("You can call a command by typing a prefix of its name, e.g. 'h' for 'hello'")
    elif fct := [*(fct for name, fct in commands.items() if name.startswith(cmd)), None][0]:
        try:
            fct(*args)
        except Exception as e:
            print(e)
            traceback.print_exc()
    else:
        print("Unknown command.", help_msg)
