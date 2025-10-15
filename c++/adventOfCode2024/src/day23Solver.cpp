#include "day23Solver.h"

#include <iostream>
#include <map>
#include <solver.h>
#include <string>
#include <algorithm>
#include <iterator>
#include <set>
#include <vector>
#include <cstdint>

using namespace std;

static NodeKey INITIAL('a' << 5 | 'a');

static NodeKey getKey(const string &l, int start) {
  NodeKey first((l[start] - 'a') << 5);
  NodeKey second(l[start + 1] - 'a');
  return (first | second);
}

static char getFirstPart(NodeKey key) {
    return 'a' + (char)(key >> 5);
}

static string toString(NodeKey key) {
  string r;
  r += getFirstPart(key);
  r += 'a' + (char)(key & 0x1f);
  return r;
}

template <typename T> class bronKerbosch {
  vector<set<T>> m_graph;
  set<T> m_nodes;
  set<T> m_maxClique;

  public:
  void addVertex(T a, T b) {
    T maxNode(max(a + 1, b + 1));
    m_graph.resize(max((T)m_graph.capacity(), maxNode));
    m_graph[a].insert(b);
    m_graph[b].insert(a);
    m_nodes.insert(a);
    m_nodes.insert(b);
  }

  set<T> computeMaxClique() {
    set<T> empty{};
    //BronKerboschPivot(empty, m_nodes, empty, m_graph);
    internalCompute(empty, m_nodes, empty);
    return m_maxClique;
  }

  private:
  void internalCompute(set<T> maxClique, set<T> candidates, set<T> processed) {
    if (candidates.empty() && processed.empty()) {
      // Base case: both candidates and processed are empty, R is a maximal clique
      if (maxClique.size() > m_maxClique.size()) {
        m_maxClique = maxClique;
      }
      return;
    }

    // Choose a pivot vertex from candidates or processed
    T pivot = !candidates.empty() ? *candidates.begin() : *processed.begin();

    // Iterate through vertices in candidates \ N(pivot)
    set<T> non_neighbors;
    for (T v : candidates) {
      if (m_graph[pivot].count(v) == 0) {
        non_neighbors.insert(v);
      }
    }

    for (T v : non_neighbors) {
      set<T> newMaxClique(maxClique);
      newMaxClique.insert(v);

      set<T> newCandidates, newProcessed;
      const set<T> &neighbors = m_graph[v];

      for (T u : candidates) {
        if (neighbors.count(u) > 0) {
          newCandidates.insert(u);
        }
      }

      for (T u : processed) {
        if (neighbors.count(u) > 0) {
          newProcessed.insert(u);
        }
      }

      // Recursive call
      internalCompute(newMaxClique, newCandidates, newProcessed);

      // Move vertex v from candidates to processed
      candidates.erase(v);
      processed.insert(v);
    }
  }
};

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
  if (second == -1) {
    for (NodeKey forwardLink : forward.find(start)->second) {
      getTripleSet(dest, start, forward, reverse, forwardLink);
    }
  } else if (third == -1) {
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

string day23Solver::computeString() {
  bronKerbosch<NodeKey> graph;

  for (const string &l : m_data) {
    graph.addVertex(getKey(l, 0), getKey(l, 3));
  }
  set<NodeKey> best(graph.computeMaxClique());
  string r;

  for (const NodeKey t : best) {
    if (!r.empty()) {
      r += ',';
    }
    r += toString(t);
  }

    return r;
}

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