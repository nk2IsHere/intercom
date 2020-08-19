#!/usr/bin/env python3

import argparse
import subprocess
import re
import os

def find_previous_tag():
    # NOTE if changing any part of this composite shell command, make sure gradle/functions.gradle#getLastTagInfo
    # is changed accordingly

    p1 = subprocess.Popen(["git", "tag"], stdout=subprocess.PIPE)
    p2 = subprocess.Popen(["grep", '\(dev\|internal\|external\|release\)'], stdin = p1.stdout, stdout=subprocess.PIPE)
    p3 = subprocess.Popen(["awk", '-F[.-]', "{print $(NF-1),$0}"], stdin = p2.stdout, stdout=subprocess.PIPE)
    p4 = subprocess.Popen(["sort", "-nr"], stdin = p3.stdout, stdout=subprocess.PIPE)
    p5 = subprocess.Popen(["awk", "{print $2}"], stdin = p4.stdout, stdout=subprocess.PIPE)
    p6 = subprocess.Popen(["head", "-n", "8"], stdin = p5.stdout, stdout=subprocess.PIPE)
    p1.stdout.close()
    p2.stdout.close()
    p3.stdout.close()
    p4.stdout.close()
    p5.stdout.close()
    output,err = p6.communicate()
    tag = output.decode('UTF-8').rstrip()
    return tag if tag else None

# Parses a tag of format "v4.14[.1].384-internal"
#                          |  |  |   |     |
#                          |  |  |   |     \-- build type
#                          |  |  |   \-- build number
#                          |  |  \-- hotfix version (OPTIONAL!)
#                          |  \-- minor version
#                          \- major version
def parse_tag(tag):
    match = re.search("v(\d+)\.(\d+)(\.(\d+))?\.(\d+)[-.](dev|internal|external|release)", tag)
    return {
        'major_version': match.group(1),
        'minor_version': match.group(2),
        'hotfix_version': match.group(4),
        'build_number': match.group(5),
        'build_type': match.group(6)
    } if match else None

def increase_version(previous_tag_info, build_type, inc_major, inc_minor, inc_hotfix):
    # previous tag could have different build type, adjust it
    tag_info = previous_tag_info.copy()
    tag_info['build_type'] = build_type
    if inc_major:
        return increase_major(tag_info)
    elif inc_minor:
        return increase_minor(tag_info)
    elif inc_hotfix:
        return increase_hotfix(tag_info)
    else:
        return increase_build(tag_info)

def increase_hotfix(tag_info):
    upd = tag_info.copy()
    if upd['hotfix_version']:
        upd['hotfix_version'] = str(int(upd['hotfix_version']) + 1)
    else:
        upd['hotfix_version'] = '1'
    upd['build_number'] = str(int(upd['build_number']) + 1)
    return upd

def increase_minor(tag_info):
    upd = tag_info.copy()
    upd['minor_version'] = str(int(upd['minor_version']) + 1)
    upd['hotfix_version'] = None
    upd['build_number'] = str(int(upd['build_number']) + 1)
    return upd

def increase_major(tag_info):
    upd = tag_info.copy()
    upd['major_version'] = str(int(upd['major_version']) + 1)
    upd['minor_version'] = '0'
    upd['hotfix_version'] = None
    upd['build_number'] = str(int(upd['build_number']) + 1)
    return upd

def increase_build(tag_info):
    upd = tag_info.copy()
    upd['build_number'] = str(int(upd['build_number']) + 1)
    return upd

def assemble_version(tag_info):
    if tag_info['hotfix_version']:
        return "v%s.%s.%s.%s-%s" % (
            tag_info['major_version'],
            tag_info['minor_version'],
            tag_info['hotfix_version'],
            tag_info['build_number'],
            tag_info['build_type'])
    else:
        return "v%s.%s.%s-%s" % (
            tag_info['major_version'],
            tag_info['minor_version'],
            tag_info['build_number'],
            tag_info['build_type'])

def create_first_version(build_type):
    ans = str(input("This is the first time %s build type tag is created.\nWhich base version to use? (default=\"0.1\") " % (build_type)))
    if ans:
        major, minor = ans.split(".")
    else:
        major, minor = ['0', '1']
    return {
        'major_version': major,
        'minor_version': minor,
        'hotfix_version': None,
        'build_number': '1',
        'build_type': build_type
    }

def ask_to_choose(question, variants):
    while "the answer is invalid":
        v1 = variants[0]
        v2 = variants[1]
        q = "%s\n\nAnswer (%s/%s): " % (question, v1, v2)
        reply = str(input(q)).lower().strip()
        if reply[:1] == v1:
            return True
        if reply[:1] == v2:
            return False
        print("Please answer '%s' or '%s'" % (v1, v2))

def raise_KeyError(msg=''): raise KeyError(msg)

def env_flutter(tag, build_number):
    tag = tag[1:] # cutting of 'v' part, because flutter does not support it

    if os.path.exists('pubspec.yaml'):
        print('Will add {tag} to pubspec.yaml'.format(tag=tag))
        with open('pubspec.yaml', 'r') as pubspecRead:
            pubspecContents = pubspecRead.read()
            pubspecRead.close()

            pubspecContents = re.sub(r'version: [0-9\.\-a-z\+]*', 'version: {version}+{buildNumber}'.format(version=tag, buildNumber=build_number), pubspecContents)
            with open('pubspec.yaml', 'w') as pubspecWrite:
                pubspecWrite.write(pubspecContents)
                pubspecWrite.close()

        subprocess.run(["flutter", "pub", "get"])
        subprocess.run(["git", "add", "pubspec.yaml"])
        subprocess.run(["git", "commit", "-m", '"Update version of pubspec.yaml to {version}"'.format(version=tag)])
    else:
        print('pubspec.yaml is not found, this script must be located in the root of project')

def env_none(tag, build_number):
    pass

def env_get_corresponding(type):
    return {
        'flutter': env_flutter,
        None: env_none
    }[type]

def get_git_has_uncommited_files():
  gitCommitClearnessCheck = subprocess.Popen(["git", "diff", "--quiet", "--cached", "--exit-code"], stdout=subprocess.PIPE)
  gitCommitClearnessCheck.communicate()[0]
  return True if gitCommitClearnessCheck.returncode != 0 else False

def main():
    parser = argparse.ArgumentParser(description='Runs "git tag" after generating a tag name for next build')
    parser.add_argument('buildtype',
                        choices=['dev', 'internal', 'external', 'release'],
                        help='requested build type')
    parser.add_argument('-hh', '--hotfix',
                        action='store_true',
                        help='increase hotfix version')
    parser.add_argument('-m', '--minor',
                        action='store_true',
                        help='increase minor version')
    parser.add_argument('-M', '--major',
                        action='store_true',
                        help='increase major version')
    parser.add_argument('-t', '--tag',
                        help='tag to use as the tag of previous build. Will be found automatically if not specified')

    parser.add_argument('-e', '--env',
                        choices=['flutter', 'spring-java', 'spring-kotlin'],
                        help='current development environment (used to put tag versions into corresponding build scripts)')

    args = parser.parse_args()
    previous_tag = args.tag if args.tag != None else find_previous_tag()
    next_version = None
    previous_version = None
    if previous_tag:
        previous_version = parse_tag(previous_tag)
        if previous_version:
            next_version = increase_version(
                previous_version,
                args.buildtype,
                args.major,
                args.minor,
                args.hotfix
            )
        else:
            raise RuntimeError("failed to parse tag " + previous_tag)
    else:
        next_version = create_first_version(args.buildtype)

    print()
    next_assembled = assemble_version(next_version)

    if not get_git_has_uncommited_files():
        if previous_version:
            print("Previous version is: %s" % (assemble_version(previous_version)))
        print("Next version is:     %s" % (next_assembled))
        print()
        command = ["git", "tag", next_assembled]
        if ask_to_choose("Confirm execution of command '%s' and adding it to build script" % " ".join(command), "yn"):
            env_get_corresponding(args.env)(next_assembled, next_version['build_number'])
            subprocess.run(command)
        else:
            print("Command not executed")
    else:
        print('Git currently has some uncommited files, aborting')

main()
