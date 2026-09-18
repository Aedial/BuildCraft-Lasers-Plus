#!/usr/bin/env python3
"""
Compare selected methods from a pair of Spark sampler reports.
"""

import argparse
import json
import re
import sys

from pathlib import Path

import urllib.error
import urllib.request


SPARK_ID_PATTERN = re.compile(r"[A-Za-z0-9_-]+")
SPARK_URL = "https://spark.lucko.me/{spark_id}?raw=true&full=true"


class SparkError(Exception):
    pass


parser = argparse.ArgumentParser(
    description="Compare selected methods from two Spark sampler reports.",
)

parser.add_argument("before_id", help="Spark ID from before the change")
parser.add_argument("after_id", help="Spark ID from after the change")
parser.add_argument(
    "--prefix",
    action="append",
    required=True,
    metavar="NAMESPACE",
    help="Class namespace to include, repeat for several mods",
)
parser.add_argument(
    "--cache-dir",
    type=Path,
    default=Path(".spark-cache"),
    help="Directory for raw Spark JSON files, default: .spark-cache",
)
parser.add_argument(
    "--refresh",
    action="store_true",
    help="Fetch both reports again even when cached data exists",
)
parser.add_argument(
    "--timeout",
    type=float,
    default=30.0,
    help="HTTP timeout in seconds, default: 30",
)
parser.add_argument(
    "--limit",
    type=int,
    default=0,
    metavar="COUNT",
    help="Show this many methods in each table, default: every method",
)


def validate_spark_id(spark_id):
    if not SPARK_ID_PATTERN.fullmatch(spark_id):
        raise SparkError("Spark IDs may only contain letters, digits, underscores, and hyphens")


def normalize_prefixes(prefixes):
    normalized = []
    for prefix in prefixes:
        prefix = prefix.rstrip(".")
        if not prefix:
            raise SparkError("Namespace prefixes cannot be empty")

        if prefix not in normalized:
            normalized.append(prefix)

    return normalized


def parse_report(payload, source):
    try:
        report = json.loads(payload)
    except json.JSONDecodeError as error:
        raise SparkError(f"{source} does not contain valid JSON: {error}")

    if not isinstance(report, dict) or not isinstance(report.get("threads"), list):
        raise SparkError(f"{source} is not a Spark sampler report with thread data")

    return report


def fetch_report(spark_id, timeout):
    url = SPARK_URL.format(spark_id=spark_id)
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "spark-namespace-compare/1.0"},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.read().decode("utf-8")
    except (urllib.error.HTTPError, urllib.error.URLError, UnicodeDecodeError) as error:
        raise SparkError(f"Could not fetch {url}: {error}")


def load_report(spark_id, cache_dir, refresh, timeout):
    cache_file = cache_dir / f"{spark_id}.json"
    if cache_file.exists() and not refresh:
        try:
            return parse_report(cache_file.read_text(encoding="utf-8"), str(cache_file)), cache_file
        except (OSError, SparkError) as error:
            print(f"Ignoring invalid cache file {cache_file}: {error}", file=sys.stderr)

    payload = fetch_report(spark_id, timeout)
    report = parse_report(payload, f"Spark report {spark_id}")
    try:
        cache_dir.mkdir(parents=True, exist_ok=True)
        temporary_file = cache_file.with_suffix(".tmp")
        temporary_file.write_text(payload, encoding="utf-8")
        temporary_file.replace(cache_file)
    except OSError as error:
        raise SparkError(f"Could not cache {cache_file}: {error}")

    return report, cache_file


def detect_mode(report, source):
    thread_names = [str(thread.get("name", "")) for thread in report["threads"]]
    has_client_thread = any("client thread" in name.lower() for name in thread_names)
    has_server_thread = any("server thread" in name.lower() for name in thread_names)
    if has_client_thread == has_server_thread:
        names = ", ".join(thread_names) or "no named threads"
        raise SparkError(f"Could not identify a single client or server mode for {source}: {names}")

    return "client" if has_client_thread else "server"


def node_time(node):
    value = node.get("time", 0)
    if isinstance(value, (int, float)):
        return value

    return 0


def matches_prefix(node, prefix):
    class_name = node.get("className")
    return isinstance(class_name, str) and \
        (class_name == prefix or class_name.startswith(prefix + "."))


def children(node):
    value = node.get("children", [])
    return value if isinstance(value, list) else []


def method_key(node):
    return (
        node["className"],
        str(node.get("methodName", "<unknown>")),
        str(node.get("methodDesc", "")),
    )


def collect_method_times(nodes, prefixes, method_times):
    for node in nodes:
        if any(matches_prefix(node, prefix) for prefix in prefixes):
            key = method_key(node)
            method_times[key] = method_times.get(key, 0) + node_time(node)

        collect_method_times(children(node), prefixes, method_times)


def total_time(report):
    return sum(node_time(thread) for thread in report["threads"])


def percent(value, whole):
    return 0.0 if whole == 0 else 100.0 * value / whole


def format_change(before, after):
    if after == 0 and before:
        return "removed"

    if before == 0:
        return "new" if after else "+0.00%"

    return f"{100.0 * (after - before) / before:+.2f}%"


def method_label(key, ambiguous_names):
    class_name, method_name, descriptor = key
    name = f"{class_name}.{method_name}"

    return f"{name} {descriptor}" if name in ambiguous_names else name


def render_table(title, rows):
    print(f"## {title}")
    print()

    headers = ("Method", "Before %", "After %", "Delta pp", "Change %")
    formatted_rows = [
        (
            method,
            "{:.3f}%".format(before),
            "{:.3f}%".format(after),
            "{:+.3f}".format(after - before),
            format_change(before, after),
        ) for method, before, after in rows
    ]

    print("| {} |".format(" | ".join(headers)))
    print("| :--- | ---: | ---: | ---: | ---: |")
    for row in formatted_rows:
        escaped = [value.replace("|", "\\|").replace("\n", "<br>") for value in row]
        print("| {} |".format(" | ".join(escaped)))


def limit_rows(rows, limit):
    return rows[:limit] if limit else rows


def compare(arguments):
    validate_spark_id(arguments.before_id)
    validate_spark_id(arguments.after_id)
    prefixes = normalize_prefixes(arguments.prefix)
    if arguments.limit < 0:
        raise SparkError("The method limit cannot be negative")

    before, before_cache = load_report(
        arguments.before_id, arguments.cache_dir, arguments.refresh, arguments.timeout
    )

    after, after_cache = load_report(
        arguments.after_id, arguments.cache_dir, arguments.refresh, arguments.timeout
    )

    before_mode = detect_mode(before, before_cache)
    after_mode = detect_mode(after, after_cache)
    if before_mode != after_mode:
        raise SparkError(f"Spark modes differ: {arguments.before_id} is {before_mode}, "
                         f"while {arguments.after_id} is {after_mode}")

    before_total = total_time(before)
    after_total = total_time(after)
    if before_total == 0 or after_total == 0:
        raise SparkError("Both reports need positive sampled thread time")

    before_methods = {}
    after_methods = {}
    collect_method_times(before["threads"], prefixes, before_methods)
    collect_method_times(after["threads"], prefixes, after_methods)

    method_names = {}
    for key in set(before_methods) | set(after_methods):
        name = f"{key[0]}.{key[1]}"
        method_names.setdefault(name, set()).add(key[2])

    ambiguous_names = {
        name for name, descriptors in method_names.items() if len(descriptors) > 1
    }

    retained_rows = []
    removed_rows = []
    new_rows = []
    for key in set(before_methods) | set(after_methods):
        before_percent = percent(before_methods.get(key, 0), before_total)
        after_percent = percent(after_methods.get(key, 0), after_total)
        row = (method_label(key, ambiguous_names), before_percent, after_percent)
        if before_percent and not after_percent:
            removed_rows.append(row)
        elif after_percent and not before_percent:
            new_rows.append(row)
        else:
            retained_rows.append(row)

    retained_rows.sort(key=lambda row: (-row[1], row[0]))
    removed_rows.sort(key=lambda row: (-row[1], row[0]))
    new_rows.sort(key=lambda row: (-row[2], row[0]))

    print("# Spark report")
    print()
    print("## Summary")
    print(f"### Mode: {before_mode}")
    print(f"### Before cache: {before_cache}")
    print(f"### After cache:  {after_cache}")
    print()

    render_table("Retained methods", limit_rows(retained_rows, arguments.limit))

    if removed_rows:
        print()
        render_table("Removed methods", limit_rows(removed_rows, arguments.limit))

    if new_rows:
        print()
        render_table("New methods", limit_rows(new_rows, arguments.limit))


def main(argv: list[str]):
    try:
        compare(parser.parse_args(argv))
    except SparkError as error:
        print("Error: {}".format(error), file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
