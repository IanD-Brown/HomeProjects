#include "day23Solver.h"

#include <cstdint>
#include <iostream>
#include <map>
#include <solver.h>
#include <string>

using namespace std;

static NodeKey getKey(const string &l, int start) {
  return l[start] << 8 | l[start + 1];
}

static char getFirstPart(NodeKey key) { return (char)(key >> 8); }

static string toString(NodeKey key) {
    string r;
  r += getFirstPart(key);
    r += (char)(key & 0xff);
  return r;
}

static void buildNodeLinks(NodeLinks &dest, NodeKey a, NodeKey b) {
  auto fnd(dest.find(a));
  if (fnd == dest.end()) {
    dest[a] = {b};
  } else {
    fnd->second.insert(b);
  }
}

day23Solver::day23Solver(const string &testFile) : solver(testFile) {}

void day23Solver::loadData(const string &line) { 
    m_data.push_back(line); 
}

void day23Solver::clearData() {
    m_data.clear(); 
}

void day23Solver::getTripleSet(NodeTripleSet &dest, NodeKey start,
                               const NodeLinks &forward,
                               const NodeLinks &reverse,
                               NodeKey second, 
                               NodeKey third) {
  if (second == 0) {
    for (NodeKey forwardLink : forward.find(start)->second) {
      getTripleSet(dest, start, forward, reverse, forwardLink);
    }
  } else if (third == 0) {
    auto fnd(forward.find(second));
    if (fnd != forward.end()) {
      for (NodeKey forwardLink : fnd->second) {
        getTripleSet(dest, start, forward, reverse, second, forwardLink);
      }
    }
    fnd = reverse.find(second);
    if (fnd != reverse.end()) {
      for (NodeKey reverseLink : fnd->second) {
        getTripleSet(dest, start, forward, reverse, second, reverseLink);
      }
    }
  } else {
    auto fnd(forward.find(third));
    if (fnd != forward.end()) {
      for (NodeKey forwardLink : fnd->second) {
        if (forwardLink == start) {
          NodeTriple value({start, second, third});
          dest.emplace(value);
        }
      }
    }
    fnd = reverse.find(third);
    if (fnd != reverse.end()) {
      for (NodeKey reverseLink : fnd->second) {
        if (reverseLink == start) {
          NodeTriple value({start, second, third});
          dest.emplace(value);
        }
      }
    }
  }
}

solveResult day23Solver::compute() {
  solveResult r(0);
  NodeLinks forward{}, reverse{};

  for (const string &l : m_data) {
    NodeKey first(getKey(l, 0));
    NodeKey second(getKey(l, 3));
    buildNodeLinks(forward, first, second);
    buildNodeLinks(reverse, second, first);
  }

  NodeTripleSet nodeTriples;
  for (const auto &it : forward) {
    getTripleSet(nodeTriples, it.first, forward, reverse);
  }

  for (const NodeTriple &nodeTriple : nodeTriples) {
    bool match(false);
    for (const NodeKey key : nodeTriple) {
      if (getFirstPart(key) == 't') {
        match = true;
        break;
      }
    }
    if (match) {
      ++r;
    }
  }

  return r;
}

string day23Solver::computeString() { return ""; }

void day23Solver::loadTestData() {
  m_test = true;

  clearData();

  loadData("kh-tc");
  loadData("qp-kh");
  loadData("de-cg");
  loadData("ka-co");
  loadData("yn-aq");
  loadData("qp-ub");
  loadData("cg-tb");
  loadData("vc-aq");
  loadData("tb-ka");
  loadData("wh-tc");
  loadData("yn-cg");
  loadData("kh-ub");
  loadData("ta-co");
  loadData("de-co");
  loadData("tc-td");
  loadData("tb-wq");
  loadData("wh-td");
  loadData("ta-ka");
  loadData("td-qp");
  loadData("aq-cg");
  loadData("wq-ub");
  loadData("ub-vc");
  loadData("de-ta");
  loadData("wq-aq");
  loadData("wq-vc");
  loadData("wh-yn");
  loadData("ka-de");
  loadData("kh-ta");
  loadData("co-tc");
  loadData("wh-qp");
  loadData("tb-vc");
  loadData("td-yn");
}