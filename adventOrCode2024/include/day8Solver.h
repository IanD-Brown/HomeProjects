#pragma once
#include <map>
#include <set>

#include "solver.h"

class day8Solver : public solver {
public:
	typedef std::pair<size_t, size_t> node;

	struct grid {
		size_t m_rowCount = 0;
		size_t m_colCount = 0;
		std::map<char, std::vector<node>> m_data;
		std::set<node> m_visited;

		void addNode(char v, size_t c) {
			node it = std::make_pair(m_rowCount, c);
			auto& fnd = m_data.find(v);

			if ( fnd == m_data.end() ) {
				m_data[ v ] = { it };
			} else {
				fnd->second.push_back(it);
			}
		}

		bool inGrid(int r, int c) {
			if ( r >= 0 && r < m_rowCount && c >= 0 && c < m_colCount ) {
				m_visited.insert(std::make_pair(( size_t ) r, ( size_t ) c));
				return true;
			}
			return false;
		}

		void clear() {
			m_rowCount = 0;
			m_colCount = 0;
			m_data.clear();
			m_visited.clear();
		}
	};
private:
	grid m_grid;

    virtual solveResult compute();
    virtual solveResult compute2();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);

protected:

public:
    day8Solver(const std::string& testFile);
};

