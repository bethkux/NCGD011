import os
from collections import defaultdict

import pandas as pd
from matplotlib import pyplot as plt

import linearity


def run():
    data = pd.read_csv("src/main/python/data/metrics.csv")
    data = preprocess_data(data)
    plt.hist2d(data.iloc[:, 1], data.iloc[:, 2], bins=20, range=[(0.0, 1.1), (0, 40)])
    plt.xlabel(data.columns[1])
    plt.ylabel(data.columns[2])

    if not os.path.exists("src/main/python/out"):
        os.makedirs("src/main/python/out")

    plt.savefig("src/main/python/out/generator.png")


def preprocess_data(data):
    metric_names = list(data.columns[1:])

    # dictionary that contains methods which parse metrics
    handlers = defaultdict(lambda: float)
    handlers["linearity"] = linearity.handle

    new_data = pd.DataFrame(columns=data.columns, index=data.index)
    for i, row in data.iterrows():
        new_data.loc[i, "run"] = row["run"]
        for metric_name in metric_names:
            new_data.loc[i, metric_name] = handlers[metric_name](row[metric_name])

    return new_data


if __name__ == "__main__":
    run()
    run()
