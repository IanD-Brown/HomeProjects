#pragma once

#include <string>
#include <vector>

using solveResult = long long;
using coordinate = std::pair<size_t, size_t>;

#define COORD_ROW(p) p.first
#define COORD_COL(p) p.second

class solver  {
    private:
        std::string m_testFile;
        
    protected:
		bool m_part1;
		bool m_test;
        virtual void clearData() = 0;

        virtual void loadData(const std::string& line) = 0;

        std::vector<int> asVectorInt(const std::string& line,
                                     const std::string& delimiter);

        void loadFromFile(const std::string& file);

		void assertEquals(size_t actual, size_t expected, const std::string& message);

    public:
        solver(const std::string& testFile);

        void loadFromFile();

        virtual solveResult compute() = 0;
		virtual solveResult compute2();

        virtual void loadTestData() = 0;

        std::string getDay();

		virtual std::string computeString() { return ""; }
		virtual std::string computeString2() {
			m_part1 = false;

			return computeString(); 
		}
};
