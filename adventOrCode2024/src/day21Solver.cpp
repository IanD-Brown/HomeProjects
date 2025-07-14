#include "day21Solver.h"

#include <iostream>
#include <queue>
#include <set>

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

template <typename T> class Graph {
public:
	T m_nodeCount;
	vector<vector<pair<T, int>>> m_edges;
	map<T, set<T>> m_bestNextNode;
	T m_src;

	Graph(T nodeCount) : m_edges(nodeCount), m_src(-1) {
		m_nodeCount = nodeCount;
	}

	void addEdge(T from, T to, int weight) {
		addEdge(from, to, weight, weight);
	}

	void addEdge(T from, T to, int weight, int reverseWeight) {
		m_edges[from].push_back(make_pair(to, weight));
		m_edges[to].push_back(make_pair(from, reverseWeight));
	}

	void dijkstra(T src) {
		if (m_src != src) {
			m_src = src;
			priority_queue<pair<int, T>, vector<pair<int, T>>, greater<pair<int, T>>> pq;
			vector<int> dist(m_nodeCount, numeric_limits<int>::max());

			pq.push(make_pair(0, src));
			dist[src] = 0;

			while (!pq.empty()) {
				T from = pq.top().second;
				pq.pop();

				for (const auto &it : m_edges[from]) {
					T to(it.first);
					int weight(it.second + dist[from]);

					if (weight < dist[to]) {
						dist[to] = weight;
						m_bestNextNode[to] = {from};
						pq.push(make_pair(dist[to], to));
					} else if(weight == dist[to]) {
						m_bestNextNode[to].insert(from);
					}
				}
			}
		}
	}

	vector<vector<T>> pathsTo(T dest) {
		vector<vector<T>> paths;

		if (dest == m_src) {
			paths = {{m_src}};
		} else {
			for (T nextNode : m_bestNextNode[dest]) {
				for (vector<T> nextNodePath : pathsTo(nextNode)) {
					vector<T> pathToAdd(nextNodePath);
					pathToAdd.insert(pathToAdd.begin(), dest);
					paths.push_back(pathToAdd);
				}
			}
		}

		return paths;
	}

};

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

struct ControlPad {
	Graph<size_t> m_graph;

	ControlPad(): m_graph(5) {
		m_graph.addEdge(0, 1, 1);
		m_graph.addEdge(0, 3, 1);
		m_graph.addEdge(1, 4, 1);
		m_graph.addEdge(2, 3, 1);
		m_graph.addEdge(3, 4, 1);

		m_graph.dijkstra(1);
	}

	size_t indexOf(char c) {
		switch(c) {
		case '^':
			return 0;
		case 'A':
			return 1;
		case '<':
			return 2;
		case 'v':
			return 3;
		case '>':
			return 4;
		}
		exit(666);
	}

	void addMove(string &s, int i, const vector<size_t> &path) const {
		size_t b(path[i - 1]);
		switch (path[i]) {
		case 0:
			s += b == 1 ? '>' : 'v';
			break;
		case 1:
			s += b == 0 ? '<' : 'v';
			break;
		case 2:
			s += '>';
			break;
		case 3:
			if (b == 0) {
				s += '^';
			} else if (b == 2) {
				s += '<';
			} else {
				s += '>';
			}
			break;
		case 4:
			s += b == 1 ? '^' : '<';
			break;
		}
	}

	string bestMoves(size_t index) {
		vector<vector<size_t>> paths(m_graph.pathsTo(index));
		size_t minChanges(SIZE_MAX);
		string result;

		for (const vector<size_t> &path : paths) {
			string s;
			for (int i = path.size() - 1; i > 0; --i) {
				addMove(s, i, path);
			}
			s += 'A';

			char previous(' ');
			size_t changeCount(0);
			for (char c : s) {
				if (c != previous) {
					++changeCount;
					previous = c;
				}
			}
			if (changeCount < minChanges) {
				minChanges = changeCount;
				result = s;
			}
		}
		return result;
	}

	multimap<size_t, string> moves(const multimap<size_t, string> &source) {
		multimap<size_t, string> result;

		if (source.empty()) {
			result.insert({1, "A"});
			return result;
		}
		size_t startSource(m_graph.m_src);

		size_t start(source.begin()->first);
		for (auto it(source.begin()); it != source.end() && it->first == start; ++it) {
			string s;
			for (char f : it->second) {
				size_t index(indexOf(f));
				s += bestMoves(index);
				m_graph.dijkstra(index);
			}

			result.insert({s.length(), s});

			m_graph.dijkstra(startSource);
		}

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

	for (const string &l : m_data) {
		NumericPad numericPad;
		vector<ControlPad> controlPads(controlCount);
		string t;

		for (char c : l) {
			multimap<size_t, string> moves(numericPad.moves(c - '0'));

			for (auto &i : controlPads) {
				moves = i.moves(moves);
			}

			cout << "compute (" << c << ") " << moves.begin()->second << endl;
			t += moves.begin()->second;

			r += stol(l) * moves.begin()->first;
		}
		cout << "compute (" << l << ") " << t << endl;
	}
	return r;
}

void day21Solver::loadTestData() {
	//NumericPad numericPad;
	//ControlPad controlPad;
	//multimap<size_t, string> moves(numericPad.moves(0));
	//moves = controlPad.moves(moves);
	//cout << "test moves " << moves << endl;

	m_test = true;

	clearData();

	//loadData("1");

	//assertEquals(compute(), string("<v<A>>^A<vA<A>>^AAvAA<^A").length(), "single 1 (<v<A>>^A<vA<A>>^AAvAA<^A)");

	loadData("029A");
	loadData("980A");
	loadData("179A");
	loadData("456A");
	loadData("379A");
}
