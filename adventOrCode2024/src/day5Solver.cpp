#include <cassert>
#include <iostream>
#include <algorithm>

#include "day5Solver.h"

using namespace std;

day5Solver::day5Solver(const string& testFile) : solver(testFile) {
    test();
    test2();
    loadFromFile();
    assert(compute() > 3654 && " thats too low");
    assert(compute2() == 4121 && " thats wrong");
}

void day5Solver::loadData(const string& line) {
    size_t pos = line.find("|");

    if (pos != string::npos) {
        vector<int> order = asVectorInt(line, "|");
        m_orderRules.insert(make_pair(order[0], order[1]));
        return;
    } else {
        pos = line.find(",");
        if (pos != string::npos) {
            m_updatePages.push_back(asVectorInt(line, ","));
        }
    }
}

void day5Solver::clearData() {
    m_orderRules.clear();
    m_updatePages.clear();
}

static bool isInOrder(const multimap<int, int> orderRules, const vector<int> updatePage) {
    for (auto iter = updatePage.rbegin(); iter != updatePage.rend(); ++iter) {
        int v = *iter;
        for (auto iter2 = iter + 1; iter2 != updatePage.rend(); ++iter2) {
            int v2 = *iter2;
            for (auto& it = orderRules.equal_range(v).first; it != orderRules.equal_range(v).second; ++it)
                if (v2 == (*it).second) {
                    return false;
                }
        }
    }
    return true;
}

solveResult day5Solver::compute() {
    cout << m_orderRules.size() << " pages " << m_updatePages.size() << endl;
    long t = 0;

    for (auto const& updatePage : m_updatePages) {
        if (isInOrder(m_orderRules, updatePage)) {
            t += updatePage[updatePage.size() / 2];
        }
    }

    cout << "day5 " << t << endl;

    return t;
}

struct Comp {
  const multimap<int, int>& m_orderRules;
  multimap<int, int> m_reverse;

  Comp(const multimap<int, int>& orderRules) : m_orderRules(orderRules) {
    for (auto const& p : orderRules) {
      m_reverse.insert(make_pair(p.second, p.first));
    }
  }

  bool operator()(int a, int b) {
    auto range = m_orderRules.equal_range(a);

    for (auto iter = range.first; iter != range.second; ++iter) {
      if (iter->second == b) {
        return true;
      }
    }
    auto range2 = m_reverse.equal_range(a);
    for (auto iter2 = range2.first; iter2 != range2.second; ++iter2) {
      if (iter2->second == b) {
        return false;
      }
    }

    return a < b;
  }
};

solveResult day5Solver::compute2() {
    long t = 0;

    for (auto const& updatePage : m_updatePages) {
        if (!isInOrder(m_orderRules, updatePage)) {
            vector<int> v(updatePage);
            sort(v.begin(), v.end(), Comp(m_orderRules));

            t += v[v.size() / 2];
        }
    }

    cout << "day5b " << t << endl;

    return t;
}

void day5Solver::test() {
    loadTestData();
	solveResult t = compute();

    assert(t == 143 && "distance count should match");
    clearData();
}

void day5Solver::loadTestData() {
    loadFromFile("day5\\testData.txt");
}

void day5Solver::test2() {
    loadTestData();
	solveResult t = compute2();

    assert(t == 123 && "test2 count should match");
    clearData();
}
