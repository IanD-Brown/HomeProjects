#include "day21Solver.h"

#include <iostream>
#include <map>

#include "graph.h"

using namespace std;

template <typename S> ostream &operator<<(ostream &os, const vector<S> &vector) {
	// Printing all the elements using <<
	for (const auto &i : vector) {
		os << i << ' ';
	}
	return os;
}

template <typename K, typename V>
ostream &operator<<(ostream &os,const multimap<K, V> &mm) {
	for (const auto &m : mm) {
		os << m.first << '(' << m.second << "), ";
	}
	return os;
}


struct NumericPad {
	Graph<size_t> m_graph;

	NumericPad() : m_graph(11) {
		m_graph.addEdge(0, 1, 1, 2);
		m_graph.addEdge(0, 3, 1);
		m_graph.addEdge(1, 2, 1, 2);
		m_graph.addEdge(1, 4, 1);
		m_graph.addEdge(2, 5, 1);
		m_graph.addEdge(3, 4, 1, 2);
		m_graph.addEdge(3, 6, 1);
		m_graph.addEdge(4, 5, 1, 2);
		m_graph.addEdge(4, 7, 1);
		m_graph.addEdge(5, 8, 1);
		m_graph.addEdge(6, 7, 1, 2);
		m_graph.addEdge(7, 8, 1, 2);
		m_graph.addEdge(7, 9, 1);
		m_graph.addEdge(8, 10, 1);
		m_graph.addEdge(9, 10, 1, 2);

		m_graph.dijkstra(10);
	}

	size_t indexOf(int dest) {
		switch(dest) { 
		case 7:
			return 0;
		case 8:
			return 1;
		case 9:
			return 2;
		case 4:
			return 3;
		case 5:
			return 4;
		case 6:
			return 5;
		case 1:
			return 6;
		case 2:
			return 7;
		case 3:
			return 8;
		case 0:
			return 9;
		case 'A' - '0':
			return 10;
		}
		exit(666);
	}

	multimap<size_t, string> moves(int dest) {
		multimap<size_t, string> moves;
		size_t index(indexOf(dest));
		auto paths(m_graph.pathsTo(index));

		for (const vector<size_t> &path : paths) {
			string s(pathAsButtons(path) + 'A');
			moves.insert({s.length(), s});
		}
		m_graph.dijkstra(index);
		if (moves.size() > 1) {
			size_t shortest(moves.begin()->first);
			size_t minChanges(SIZE_MAX);

			for (auto it(moves.begin()); it != moves.end();) {
				if (it->first > shortest) {
					it = moves.erase(it);
				} else {
					char previous(' ');
					size_t changeCount(0);
					for (char c : it->second) {
						if (c != previous) {
							++changeCount;
							previous = c;
						}
					}
					if (changeCount <= minChanges) {
						minChanges = changeCount;
						++it;
					} else {
						it = moves.erase(it);
					}
				}
			}
		}

		return moves;
	}

	string pathAsButtons(const vector<size_t> &path) const {
		string s;
		for (int i = path.size() - 1; i > 0; --i) {
			size_t a(path[i]);
			size_t b(path[i - 1]);
			if (a / 3 == b / 3) {
				// same row
				s += a < b ? '>' : '<';
			} else {
				s += (a / 3) < (b / 3) ? 'v' : '^';
			}
		}
		return s;
	}
};

struct ControlLevel {
	char m_currentPosition;
	map<pair<char, char>, solveResult> m_moveCountCache;
	size_t m_cachHits;

	ControlLevel() : m_currentPosition('A'), m_cachHits(0) {}
};

struct ControlPad {
	vector<ControlLevel> m_levels;

	ControlPad(size_t levelCount) : m_levels(levelCount) {
	}

	string bestMoves(char f, char currentPosition) {
		switch (currentPosition) {
		case '^':
			switch (f) {
			case '^':
				return "A";
			case 'A':
				return ">A";
			case '<':
				return "v<A";
			case 'v':
				return "vA";
			case '>':
				return "v>A";
			}
			exit(600);
		case 'A':
			switch (f) {
			case '^':
				return "<A";
			case 'A':
				return "A";
			case '<':
				return "v<<A";
			case 'v':
				return "<vA";
			case '>':
				return "vA";
			}
			exit(601);
		case '<':
			switch (f) {
			case '^':
				return ">^A";
			case 'A':
				return ">>^A";
			case '<':
				return "A";
			case 'v':
				return ">A";
			case '>':
				return ">>A";
			}
			exit(602);
		case 'v':
			switch (f) {
			case '^':
				return "^A";
			case 'A':
				return "^>A";
			case '<':
				return "<A";
			case 'v':
				return "A";
			case '>':
				return ">A";
			}
			exit(603);
		case '>':
			switch (f) {
			case '^':
				return "<^A";
			case 'A':
				return "^A";
			case '<':
				return "<<A";
			case 'v':
				return "<A";
			case '>':
				return "A";
			}
			exit(604);
		}
		exit(605);
	}

	solveResult moveCount(char destination, size_t level) {
		if (level == 1) {
			string m(bestMoves(destination, m_levels[0].m_currentPosition));
			return m.size();
		}
		ControlLevel &controlLevel(m_levels[level - 1]);
		auto key(make_pair(controlLevel.m_currentPosition, destination));
		auto fnd(controlLevel.m_moveCountCache.find(key));

		if (fnd == controlLevel.m_moveCountCache.cend()) {
			string levelMoves(bestMoves(destination, controlLevel.m_currentPosition));
			ControlLevel &nextLevelControl(m_levels[level - 2]);
			solveResult totalMoves(0);
			char savedPosition(nextLevelControl.m_currentPosition);
			for (char m : levelMoves) {
				totalMoves += moveCount(m, level - 1);
				nextLevelControl.m_currentPosition = m;
			}
			nextLevelControl.m_currentPosition = savedPosition;
			fnd = controlLevel.m_moveCountCache.emplace(key, totalMoves).first;
		} else {
			++controlLevel.m_cachHits;
		}
		controlLevel.m_currentPosition = destination;
		return fnd->second;
	}

	solveResult moveCount(const multimap<size_t, string> &source, size_t level) {
		solveResult result(LLONG_MAX);

		ControlLevel &controlLevel(m_levels[level - 1]);
		size_t start(source.begin()->first);
		char startPosition(controlLevel.m_currentPosition);
		for (auto it(source.begin()); it != source.end() && it->first == start; ++it) {
			solveResult itCount(0);
			for (char t : it->second) {
				itCount += moveCount(t, level);
			}
			if (itCount < result) {
				result = itCount;
			}
		}
		controlLevel.m_currentPosition = startPosition;

		return result;
	}
};

day21Solver::day21Solver(const string &testFile):
	solver(testFile) {}

void day21Solver::loadData(const string &line) {
	m_data.push_back(line);
}

void day21Solver::clearData() {
	m_data.clear();
}

solveResult day21Solver::compute() {
	solveResult r(0);
	const size_t controlCount(m_part1 ? 2 : 25);
	NumericPad numericPad;
	ControlPad controlPads(controlCount);

	for (const string &l : m_data) {
		solveResult moveCount(0);

		for (char c : l) {
			multimap<size_t, string> moves(numericPad.moves(c - '0'));

			moveCount += controlPads.moveCount(moves, controlCount); 
		}
		r += stoll(l) * moveCount;
	}
	return r;
}

void day21Solver::loadTestData() {
	m_test = true;

	clearData();

	loadData("029A");
	loadData("980A");
	loadData("179A");
	loadData("456A");
	loadData("379A");
}
