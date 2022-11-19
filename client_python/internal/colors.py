from colorama import Fore, Style, init

# This file creates global helper functions that allow printing text in color easily.

# Example usage:
# >>> print(GREEN("Hello"), RED("World"))

# noinspection PyUnresolvedReferences
__all__ = [
    "BLACK",
    "RED",
    "GREEN",
    "YELLOW",
    "BLUE",
    "MAGENTA",
    "CYAN",
    "WHITE",
    "RESET",
    "LIGHTBLACK_EX",
    "LIGHTRED_EX",
    "LIGHTGREEN_EX",
    "LIGHTYELLOW_EX",
    "LIGHTBLUE_EX",
    "LIGHTMAGENTA_EX",
    "LIGHTCYAN_EX",
    "LIGHTWHITE_EX"
]


def printer(color):
    """Helper function that returns a function that prints a given string in the given color"""
    return lambda *args: getattr(Fore, color) + " ".join(map(str, args)) + Style.RESET_ALL


for name in __all__:
    globals()[name] = printer(name)

init()
